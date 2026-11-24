package com.dukaanai.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

sealed interface RecordingStatus {
    data object Idle : RecordingStatus
    data class Recording(
        val elapsedSeconds: Int,
        val maxSeconds: Int = 30,
        val amplitudeLevel: Float = 0f
    ) : RecordingStatus
    data class Recorded(
        val durationSeconds: Double,
        val fileSizeBytes: Long,
        val filePath: String,
        val isPlaying: Boolean = false
    ) : RecordingStatus
    data class Error(val message: String) : RecordingStatus
}

class WavAudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val MAX_RECORDING_SECONDS = 30
        private const val TEMP_WAV_FILENAME = "dukaan_voice_test_temp.wav"
    }

    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val tempAudioFile: File
        get() = File(context.cacheDir, TEMP_WAV_FILENAME)

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        // Stop any active recording or playback first
        stopRecording()
        stopPlayback()
        deleteTempFile()

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _status.value = RecordingStatus.Error("Unsupported audio configuration on this device.")
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to standard MIC audio source if VOICE_RECOGNITION fails
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _status.value = RecordingStatus.Error("Failed to initialize audio recorder.")
                return false
            }

            audioRecord?.startRecording()
        } catch (e: SecurityException) {
            _status.value = RecordingStatus.Error("Audio recording permission was not granted.")
            return false
        } catch (e: Exception) {
            _status.value = RecordingStatus.Error("Recording error: ${e.localizedMessage ?: "Unknown"}")
            return false
        }

        val targetFile = tempAudioFile
        var currentAmplitude = 0f

        // Record audio stream in background
        recordingJob = scope.launch {
            try {
                FileOutputStream(targetFile).use { fos ->
                    // Write placeholder 44-byte WAV header
                    val placeholderHeader = ByteArray(44)
                    fos.write(placeholderHeader)

                    val audioBuffer = ShortArray(bufferSize)
                    val byteBuffer = ByteBuffer.allocate(bufferSize * 2).order(ByteOrder.LITTLE_ENDIAN)
                    var totalPcmBytes = 0L

                    while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                        if (readCount > 0) {
                            byteBuffer.clear()
                            var sumSquare = 0.0
                            for (i in 0 until readCount) {
                                val sample = audioBuffer[i]
                                byteBuffer.putShort(sample)
                                sumSquare += (sample * sample).toDouble()
                            }
                            fos.write(byteBuffer.array(), 0, readCount * 2)
                            totalPcmBytes += (readCount * 2)

                            // Compute RMS amplitude (normalized between 0.0 and 1.0)
                            val rms = sqrt(sumSquare / readCount)
                            currentAmplitude = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
                        }
                    }

                    fos.flush()

                    // Rewrite proper WAV header with actual data size
                    writeWavHeader(targetFile, totalPcmBytes)
                }
            } catch (e: Exception) {
                if (isActive) {
                    _status.value = RecordingStatus.Error("Error saving audio: ${e.localizedMessage}")
                }
            }
        }

        // Timer job with max 30-second safeguard
        timerJob = scope.launch(Dispatchers.Main) {
            var elapsed = 0
            while (isActive && elapsed < MAX_RECORDING_SECONDS) {
                _status.value = RecordingStatus.Recording(
                    elapsedSeconds = elapsed,
                    maxSeconds = MAX_RECORDING_SECONDS,
                    amplitudeLevel = currentAmplitude
                )
                delay(1000)
                elapsed++
            }

            // Auto stop after 30 seconds
            if (isActive) {
                stopRecording()
            }
        }

        return true
    }

    fun stopRecording() {
        val wasRecording = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING
        timerJob?.cancel()
        timerJob = null

        try {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.stop()
                audioRecord?.release()
            }
        } catch (_: Exception) {
        } finally {
            audioRecord = null
        }

        scope.launch {
            recordingJob?.join()
            recordingJob = null

            if (wasRecording) {
                val file = tempAudioFile
                if (file.exists() && file.length() > 44) {
                    val pcmBytes = file.length() - 44
                    val duration = pcmBytes.toDouble() / (SAMPLE_RATE * 2)
                    _status.value = RecordingStatus.Recorded(
                        durationSeconds = duration,
                        fileSizeBytes = file.length(),
                        filePath = file.absolutePath,
                        isPlaying = false
                    )
                } else {
                    _status.value = RecordingStatus.Error("No audio was recorded.")
                }
            }
        }
    }

    fun playRecordedAudio() {
        val current = _status.value
        if (current !is RecordingStatus.Recorded) return
        val file = File(current.filePath)
        if (!file.exists()) {
            _status.value = RecordingStatus.Error("Audio file no longer exists.")
            return
        }

        stopPlayback()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _status.value = current.copy(isPlaying = false)
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
            _status.value = current.copy(isPlaying = true)
        } catch (e: Exception) {
            _status.value = RecordingStatus.Error("Playback failed: ${e.localizedMessage}")
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            val current = _status.value
            if (current is RecordingStatus.Recorded) {
                _status.value = current.copy(isPlaying = false)
            }
        }
    }

    fun clear() {
        stopRecording()
        stopPlayback()
        deleteTempFile()
        _status.value = RecordingStatus.Idle
    }

    fun deleteTempFile() {
        try {
            val file = tempAudioFile
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Writes a standard 44-byte RIFF/WAVE header to the target file.
     */
    private fun writeWavHeader(file: File, pcmDataLength: Long) {
        val totalDataLen = pcmDataLength + 36
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            val header = ByteArray(44)

            // "RIFF"
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()

            // Total file size - 8
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()

            // "WAVE"
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()

            // "fmt " chunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()

            // 16 for PCM format
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0

            // Audio format (1 = PCM)
            header[20] = 1
            header[21] = 0

            // Number of channels (1 = Mono)
            header[22] = channels.toByte()
            header[23] = 0

            // Sample rate (16000)
            header[24] = (SAMPLE_RATE and 0xff).toByte()
            header[25] = ((SAMPLE_RATE shr 8) and 0xff).toByte()
            header[26] = ((SAMPLE_RATE shr 16) and 0xff).toByte()
            header[27] = ((SAMPLE_RATE shr 24) and 0xff).toByte()

            // Byte rate (SampleRate * Channels * BitsPerSample / 8)
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()

            // Block align (Channels * BitsPerSample / 8)
            header[32] = (channels * 2).toByte()
            header[33] = 0

            // Bits per sample (16)
            header[34] = 16
            header[35] = 0

            // "data" chunk header
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()

            // Data length
            header[40] = (pcmDataLength and 0xff).toByte()
            header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
            header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
            header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

            raf.write(header, 0, 44)
        }
    }
}
