package com.dukaanai.app.audio

import android.content.Context
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface TranscribeState {
    data object Idle : TranscribeState
    data object LoadingModel : TranscribeState
    data object Transcribing : TranscribeState
    data class Success(
        val text: String,
        val rawText: String? = null,
        val timeTakenSeconds: Double,
        val detectedLanguage: String = "hi"
    ) : TranscribeState
    data class Error(val message: String) : TranscribeState
}

interface SpeechToTextEngine {
    val state: StateFlow<TranscribeState>
    val currentLanguage: StateFlow<String>
    fun setTargetLanguage(languageCode: String)
    suspend fun transcribe(audioFile: File): Result<String>
    fun clear()
    fun release()
}

class SherpaSpeechToTextEngine(private val context: Context) : SpeechToTextEngine {

    private val _state = MutableStateFlow<TranscribeState>(TranscribeState.Idle)
    override val state: StateFlow<TranscribeState> = _state.asStateFlow()

    // Default to "hi" (Hindi) as requested for kirana/shop use
    private val _currentLanguage = MutableStateFlow("hi")
    override val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var recognizer: OfflineRecognizer? = null
    private var isInitialized = false

    override fun setTargetLanguage(languageCode: String) {
        if (_currentLanguage.value != languageCode) {
            _currentLanguage.value = languageCode
            // Reset existing recognizer so the new language token is applied on next transcription
            release()
        }
    }

    private suspend fun initializeIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && recognizer != null) return@withContext true

        _state.value = TranscribeState.LoadingModel

        try {
            // Find which asset directory contains a Whisper model
            val candidateDirs = listOf("sherpa-onnx-whisper-base", "sherpa-onnx-whisper-tiny", "whisper-base", "whisper-tiny")
            var selectedDir: String? = null
            var assetFiles: Array<String> = emptyArray()

            for (dir in candidateDirs) {
                val list = try {
                    context.assets.list(dir) ?: emptyArray()
                } catch (_: Exception) {
                    emptyArray()
                }
                if (list.any { it.endsWith("-tokens.txt") || it == "tokens.txt" }) {
                    selectedDir = dir
                    assetFiles = list
                    break
                }
            }

            if (selectedDir == null) {
                _state.value = TranscribeState.Error(
                    "Multilingual Whisper model assets not found in app. Please ensure the APK was packaged with the offline model."
                )
                return@withContext false
            }

            val encoderFile = assetFiles.firstOrNull { it.contains("encoder") && it.contains("int8") && it.endsWith(".onnx") }
                ?: assetFiles.firstOrNull { it.contains("encoder") && it.endsWith(".onnx") }

            val decoderFile = assetFiles.firstOrNull { it.contains("decoder") && it.contains("int8") && it.endsWith(".onnx") }
                ?: assetFiles.firstOrNull { it.contains("decoder") && it.endsWith(".onnx") }

            val tokensFile = assetFiles.firstOrNull { it.endsWith("-tokens.txt") || it == "tokens.txt" }

            if (encoderFile == null || decoderFile == null || tokensFile == null) {
                _state.value = TranscribeState.Error(
                    "Incomplete Whisper model files in assets/$selectedDir (encoder: $encoderFile, decoder: $decoderFile, tokens: $tokensFile)."
                )
                return@withContext false
            }

            val encoderName = "$selectedDir/$encoderFile"
            val decoderName = "$selectedDir/$decoderFile"
            val tokensName = "$selectedDir/$tokensFile"

            // Target language: "hi" for Hindi, or "" for auto-detection
            val langToUse = _currentLanguage.value

            val whisperConfig = OfflineWhisperModelConfig(
                encoder = encoderName,
                decoder = decoderName,
                language = langToUse,
                task = "transcribe", // Native transcription (preserves Devanagari Hindi text)
                tailPaddings = 1000
            )

            val modelConfig = OfflineModelConfig(
                whisper = whisperConfig,
                tokens = tokensName,
                modelType = "whisper",
                numThreads = 2,
                debug = false
            )

            val recognizerConfig = OfflineRecognizerConfig(
                modelConfig = modelConfig,
                decodingMethod = "greedy_search"
            )

            recognizer = OfflineRecognizer(
                assetManager = context.assets,
                config = recognizerConfig
            )

            isInitialized = true
            return@withContext true
        } catch (e: UnsatisfiedLinkError) {
            _state.value = TranscribeState.Error(
                "Native speech recognition library could not be loaded: ${e.message}"
            )
            return@withContext false
        } catch (e: Exception) {
            _state.value = TranscribeState.Error(
                "Model initialization error: ${e.localizedMessage ?: e.message}"
            )
            return@withContext false
        }
    }

    override suspend fun transcribe(audioFile: File): Result<String> = withContext(Dispatchers.Default) {
        if (!audioFile.exists() || audioFile.length() <= 44) {
            val err = "Audio recording is empty or not found. Please record again."
            _state.value = TranscribeState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        val ready = initializeIfNeeded()
        if (!ready || recognizer == null) {
            val currentErr = (_state.value as? TranscribeState.Error)?.message
                ?: "Speech recognition model could not be initialized."
            return@withContext Result.failure(IllegalStateException(currentErr))
        }

        _state.value = TranscribeState.Transcribing

        val startTime = System.currentTimeMillis()

        try {
            val waveData = WaveReader.readWave(audioFile.absolutePath)
            if (waveData.samples.isEmpty()) {
                val err = "No audio waveform detected in the recording."
                _state.value = TranscribeState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val duration = waveData.samples.size.toDouble() / waveData.sampleRate
            if (duration < 0.3) {
                val err = "Recording is too short (less than 0.5s). Please speak a complete phrase."
                _state.value = TranscribeState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val currentRecognizer = recognizer!!
            val stream = currentRecognizer.createStream()
            stream.acceptWaveform(waveData.samples, waveData.sampleRate)

            currentRecognizer.decode(stream)
            val result = currentRecognizer.getResult(stream)
            stream.release()

            val rawTranscribed = result.text.trim()
            val correction = KiranaVoiceCorrector.process(rawTranscribed)
            val transcribed = correction.corrected
            val timeTakenSec = (System.currentTimeMillis() - startTime) / 1000.0

            if (transcribed.isBlank()) {
                val notice = "No speech recognized. Please speak clearly into the phone microphone."
                _state.value = TranscribeState.Error(notice)
                Result.success("")
            } else {
                _state.value = TranscribeState.Success(
                    text = transcribed,
                    rawText = if (correction.isModified) rawTranscribed else null,
                    timeTakenSeconds = timeTakenSec,
                    detectedLanguage = if (result.lang.isNotBlank()) result.lang else _currentLanguage.value
                )
                Result.success(transcribed)
            }
        } catch (e: Exception) {
            val errMsg = "Transcription error: ${e.localizedMessage ?: e.message}"
            _state.value = TranscribeState.Error(errMsg)
            Result.failure(e)
        }
    }

    override fun clear() {
        _state.value = TranscribeState.Idle
    }

    override fun release() {
        try {
            recognizer?.release()
        } catch (_: Exception) {}
        recognizer = null
        isInitialized = false
        _state.value = TranscribeState.Idle
    }
}
