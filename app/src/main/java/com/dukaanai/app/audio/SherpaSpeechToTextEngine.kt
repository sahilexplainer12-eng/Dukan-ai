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
        val timeTakenSeconds: Double
    ) : TranscribeState
    data class Error(val message: String) : TranscribeState
}

interface SpeechToTextEngine {
    val state: StateFlow<TranscribeState>
    suspend fun transcribe(audioFile: File): Result<String>
    fun clear()
    fun release()
}

class SherpaSpeechToTextEngine(private val context: Context) : SpeechToTextEngine {

    private val _state = MutableStateFlow<TranscribeState>(TranscribeState.Idle)
    override val state: StateFlow<TranscribeState> = _state.asStateFlow()

    private var recognizer: OfflineRecognizer? = null
    private var isInitialized = false

    private suspend fun initializeIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && recognizer != null) return@withContext true

        _state.value = TranscribeState.LoadingModel

        try {
            // Verify model files exist in assets
            val assetList = try {
                context.assets.list("whisper-tiny") ?: emptyArray()
            } catch (e: Exception) {
                emptyArray()
            }

            val hasEncoder = assetList.contains("tiny-encoder.int8.onnx") || assetList.contains("tiny-encoder.onnx")
            val hasDecoder = assetList.contains("tiny-decoder.int8.onnx") || assetList.contains("tiny-decoder.onnx")
            val hasTokens = assetList.contains("tiny-tokens.txt")

            if (!hasEncoder || !hasDecoder || !hasTokens) {
                _state.value = TranscribeState.Error(
                    "Whisper model files not found in app assets. Please rebuild the APK with the model packaged."
                )
                return@withContext false
            }

            val encoderName = if (assetList.contains("tiny-encoder.int8.onnx")) {
                "whisper-tiny/tiny-encoder.int8.onnx"
            } else {
                "whisper-tiny/tiny-encoder.onnx"
            }

            val decoderName = if (assetList.contains("tiny-decoder.int8.onnx")) {
                "whisper-tiny/tiny-decoder.int8.onnx"
            } else {
                "whisper-tiny/tiny-decoder.onnx"
            }

            val tokensName = "whisper-tiny/tiny-tokens.txt"

            // Configure Whisper multilingual offline model
            val whisperConfig = OfflineWhisperModelConfig(
                encoder = encoderName,
                decoder = decoderName,
                language = "", // Auto-detect language (handles Hindi / English code-switching)
                task = "transcribe",
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
                "Native library could not be loaded on this device architecture: ${e.message}"
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
            // Read 16kHz WAV waveform
            val waveData = WaveReader.readWave(audioFile.absolutePath)
            if (waveData.samples.isEmpty()) {
                val err = "No audio waveform detected in the recording."
                _state.value = TranscribeState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            // Check minimum duration (~0.3 seconds)
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

            val transcribed = result.text.trim()
            val timeTakenSec = (System.currentTimeMillis() - startTime) / 1000.0

            if (transcribed.isBlank()) {
                val notice = "No speech detected in audio. Please speak clearly into the microphone."
                _state.value = TranscribeState.Error(notice)
                Result.success("")
            } else {
                _state.value = TranscribeState.Success(
                    text = transcribed,
                    timeTakenSeconds = timeTakenSec
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
