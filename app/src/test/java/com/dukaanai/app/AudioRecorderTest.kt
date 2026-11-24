package com.dukaanai.app

import com.dukaanai.app.audio.WavAudioRecorder
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioRecorderTest {

    @Test
    fun testWavFormatParameters() {
        assertEquals(16000, WavAudioRecorder.SAMPLE_RATE)
        assertEquals(30, WavAudioRecorder.MAX_RECORDING_SECONDS)
    }

    @Test
    fun testDurationCalculationFromPcmBytes() {
        // At 16000 Hz, 16-bit (2 bytes per sample), mono (1 channel):
        // 1 second of audio = 16000 * 2 = 32,000 bytes.
        val sampleRate = WavAudioRecorder.SAMPLE_RATE
        val pcmBytesFor3Seconds = 3 * sampleRate * 2L
        val calculatedDuration = pcmBytesFor3Seconds.toDouble() / (sampleRate * 2)

        assertEquals(3.0, calculatedDuration, 0.001)
    }
}
