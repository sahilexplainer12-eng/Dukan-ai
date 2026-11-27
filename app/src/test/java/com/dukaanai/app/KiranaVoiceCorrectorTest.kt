package com.dukaanai.app

import com.dukaanai.app.audio.KiranaVoiceCorrector
import org.junit.Assert.assertEquals
import org.junit.Test

class KiranaVoiceCorrectorTest {

    @Test
    fun testDetergentCorrection() {
        val input = "रिजवान भाई दस रुपए का डायरेक्टर ले गए हैं"
        val expected = "रिजवान भाई दस रुपये का डिटर्जेंट ले गए हैं"
        assertEquals(expected, KiranaVoiceCorrector.correct(input))
    }

    @Test
    fun testUdhaarAndCommoditiesCorrection() {
        val input = "राजू दो केजी chawal और एक tel ले गया, एक सौ अस्सी रुपए उधर"
        val expected = "राजू दो किलो चावल और एक तेल ले गया, एक सौ अस्सी रुपये उधार"
        assertEquals(expected, KiranaVoiceCorrector.correct(input))
    }

    @Test
    fun testPaymentAndCurrencyCorrection() {
        val input = "मुन्ना ने पिछले खाते के पांच सौ rs जामा किए"
        val expected = "मुन्ना ने पिछले खाते के पाँच सौ रुपये जमा किए"
        assertEquals(expected, KiranaVoiceCorrector.correct(input))
    }

    @Test
    fun testGroceryUnitsAndItemsCorrection() {
        val input = "भैया 1kg शक्कर और आधा kg daal दे देना"
        val expected = "भैया 1 किलो चीनी और आधा किलो दाल दे देना"
        assertEquals(expected, KiranaVoiceCorrector.correct(input))
    }

    @Test
    fun testDigitSpacingCorrection() {
        val input = "10रुपये का साबुन और 2किलो आटा"
        val expected = "10 रुपये का साबुन और 2 किलो आटा"
        assertEquals(expected, KiranaVoiceCorrector.correct(input))
    }
}
