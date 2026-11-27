package com.dukaanai.app.audio

import java.util.regex.Pattern

/**
 * Domain-specific post-processing corrector for Kirana / Retail shopkeeper transactions in Hindi & Hinglish.
 * Automatically resolves common acoustic confusions and normalizes commodities, units, currency, and ledger actions.
 */
object KiranaVoiceCorrector {

    data class CorrectionResult(
        val original: String,
        val corrected: String,
        val isModified: Boolean
    )

    /**
     * Corrects raw speech recognition output into standardized Kirana transaction Hindi text.
     */
    fun correct(rawInput: String): String {
        return process(rawInput).corrected
    }

    /**
     * Processes raw speech recognition output and returns both original and corrected text with modification status.
     */
    fun process(rawInput: String): CorrectionResult {
        if (rawInput.isBlank()) return CorrectionResult(rawInput, "", false)

        var text = rawInput.trim()

        // 1. Whitespace & punctuation cleanup
        text = text.replace(Regex("\\s+"), " ")

        // 2. Standardize Currency variations to "रुपये"
        // Matches: रुपए, रूपये, रुपया, रू, रु, रु., rs, rs., rupees, rupaye
        text = text.replace(Regex("(?i)\\b(rs\\.?|rupees?|rupaye?|रू|रु\\.?|रुपए|रूपये|रुपया)\\b"), "रुपये")

        // 3. Fix acoustic misinterpretations of "डिटर्जेंट" (detergent)
        // Whisper commonly mishears "डिटर्जेंट" as "डायरेक्टर", "डाइरेक्टर", "डिटर्जन", "डिटर्जेट"
        text = text.replace(Regex("(?i)\\b(डायरेक्टर|डाइरेक्टर|डिटर्जन|डिटर्जेट|डिटेर्जेंट|detergent)\\b"), "डिटर्जेंट")

        // 4. Fix acoustic misinterpretations of "सर्फ" (surf)
        text = text.replace(Regex("(?i)\\b(शर्फ|surf|सर्फ़)\\b"), "सर्फ")

        // 5. Fix acoustic misinterpretations of "उधार" (credit)
        // In fast spoken Hindi, "उधार" often gets transcribed as "उधर" (there) or "उद्धार" (salvation)
        text = text.replace(Regex("\\b(उद्धार|उधारी)\\b"), "उधार")
        // If "उधर" appears after numbers, currency, commodities, or verbs like "गया" / "गए", it is meant to be "उधार"
        text = text.replace(Regex("(?<=\\d|रुपये|सौ|हज़ार|हजार|दस|बीस|तीस|चालीस|पचास|साठ|सत्तर|अस्सी|नब्बे|गया|गए|चावल|दाल|चीनी|तेल)\\s+उधर\\b"), " उधार")
        text = text.replace(Regex("\\bउधर(?=\\s+(?:में|खाते|लिख|चढ़ा|देना|है|कर))"), "उधार")

        // 6. Fix acoustic misinterpretations of "जमा" (deposit/payment)
        text = text.replace(Regex("\\b(जामा)\\b"), "जमा")

        // 7. Standardize Weight / Measurement Units
        text = text.replace(Regex("(?i)\\b(के\\s*जी|केजी|kilo|kg|k\\.g\\.)\\b"), "किलो")
        text = text.replace(Regex("(?i)\\b(लिटर|ltr|liter|litre)\\b"), "लीटर")
        text = text.replace(Regex("(?i)\\b(पेकेट|packet|pkt)\\b"), "पैकेट")
        text = text.replace(Regex("(?i)\\b(दर्ज़न|dozen)\\b"), "दर्जन")
        text = text.replace(Regex("(?i)\\b(ग्राम|gm|g\\.?)\\b(?=\\s|$)"), "ग्राम")

        // 8. Standardize Kirana Commodities (Hindi & Hinglish loanwords)
        text = text.replace(Regex("(?i)\\b(चाउल|chawal)\\b"), "चावल")
        text = text.replace(Regex("(?i)\\b(daal|dal)\\b"), "दाल")
        text = text.replace(Regex("(?i)\\b(chini|शक्कर)\\b"), "चीनी")
        text = text.replace(Regex("(?i)\\b(tel|oil)\\b"), "तेल")
        text = text.replace(Regex("(?i)\\b(सबुन|sabun|soap)\\b"), "साबुन")
        text = text.replace(Regex("(?i)\\b(atta|aata)\\b"), "आटा")
        text = text.replace(Regex("(?i)\\b(ghee)\\b"), "घी")
        text = text.replace(Regex("(?i)\\b(doodh|milk)\\b"), "दूध")
        text = text.replace(Regex("(?i)\\b(namak|salt)\\b"), "नमक")
        text = text.replace(Regex("(?i)\\b(masala)\\b"), "मसाला")
        text = text.replace(Regex("(?i)\\b(बिस्किट|biscuit)\\b"), "बिस्कुट")
        text = text.replace(Regex("(?i)\\b(colgate|टूथपेस्ट)\\b"), "कोलगेट")
        text = text.replace(Regex("(?i)\\b(माचीस|matchbox)\\b"), "माचिस")

        // 9. Standardize common Hindi number spellings
        text = text.replace(Regex("\\bपांच\\b"), "पाँच")
        text = text.replace(Regex("\\bछः\\b"), "छह")

        // 10. Ensure clean spacing between digits and Devanagari text (e.g. "10रुपये" -> "10 रुपये", "2किलो" -> "2 किलो")
        text = text.replace(Regex("(\\d+)([\\u0900-\\u097F]+)"), "$1 $2")
        text = text.replace(Regex("([\\u0900-\\u097F]+)(\\d+)"), "$1 $2")

        // 11. Final cleanup of multiple spaces
        text = text.replace(Regex("\\s+"), " ").trim()

        return CorrectionResult(
            original = rawInput,
            corrected = text,
            isModified = (text != rawInput.trim())
        )
    }
}
