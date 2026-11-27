package com.dukaanai.app.audio

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
     * Helper to safely replace words matching a pattern with boundary awareness for Devanagari and Latin.
     */
    private fun replaceWord(input: String, targetPattern: String, replacement: String, ignoreCase: Boolean = false): String {
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val regex = Regex("(^|[\\s\\d])(?:$targetPattern)($|[\\s\\d.,!?])", options)
        // Run twice to ensure adjacent occurrences are both replaced
        val firstPass = regex.replace(input) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val suffix = matchResult.groupValues[2]
            "$prefix$replacement$suffix"
        }
        return regex.replace(firstPass) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val suffix = matchResult.groupValues[2]
            "$prefix$replacement$suffix"
        }
    }

    /**
     * Processes raw speech recognition output and returns both original and corrected text with modification status.
     */
    fun process(rawInput: String): CorrectionResult {
        if (rawInput.isBlank()) return CorrectionResult(rawInput, "", false)

        var text = rawInput.trim().replace(Regex("\\s+"), " ")

        // 1. Standardize Currency variations to "रुपये"
        text = replaceWord(text, "rs\\.?|rupees?|rupaye?|रू|रु\\.?|रुपए|रूपये|रुपया", "रुपये", ignoreCase = true)

        // 2. Fix acoustic misinterpretations of "डिटर्जेंट" (detergent)
        text = replaceWord(text, "डायरेक्टर|डाइरेक्टर|डिटर्जन|डिटर्जेट|डिटेर्जेंट|detergent", "डिटर्जेंट", ignoreCase = true)

        // 3. Fix acoustic misinterpretations of "सर्फ" (surf)
        text = replaceWord(text, "शर्फ|surf|सर्फ़", "सर्फ", ignoreCase = true)

        // 4. Fix acoustic misinterpretations of "उधार" (credit)
        text = replaceWord(text, "उद्धार|उधारी", "उधार")
        text = text.replace(
            Regex("(\\d+|रुपये|सौ|हज़ार|हजार|दस|बीस|तीस|चालीस|पचास|साठ|सत्तर|अस्सी|नब्बे|गया|गए|चावल|दाल|चीनी|तेल)\\s+उधर($|[\\s.,!?])"),
            "$1 उधार$2"
        )
        text = text.replace(
            Regex("(^|\\s)उधर(\\s+(?:में|खाते|लिख|चढ़ा|देना|है|कर))"),
            "$1उधार$2"
        )

        // 5. Fix acoustic misinterpretations of "जमा" (deposit/payment)
        text = replaceWord(text, "जामा", "जमा")

        // 6. Standardize Weight / Measurement Units
        text = replaceWord(text, "के\\s*जी|केजी|kilo|kg|k\\.g\\.", "किलो", ignoreCase = true)
        text = replaceWord(text, "लिटर|ltr|liter|litre", "लीटर", ignoreCase = true)
        text = replaceWord(text, "पेकेट|packet|pkt", "पैकेट", ignoreCase = true)
        text = replaceWord(text, "दर्ज़न|dozen", "दर्जन", ignoreCase = true)
        text = replaceWord(text, "ग्राम|gm", "ग्राम", ignoreCase = true)

        // 7. Standardize Kirana Commodities (Hindi & Hinglish loanwords)
        text = replaceWord(text, "चाउल|chawal", "चावल", ignoreCase = true)
        text = replaceWord(text, "daal|dal", "दाल", ignoreCase = true)
        text = replaceWord(text, "chini|शक्कर", "चीनी", ignoreCase = true)
        text = replaceWord(text, "tel|oil", "तेल", ignoreCase = true)
        text = replaceWord(text, "सबुन|sabun|soap", "साबुन", ignoreCase = true)
        text = replaceWord(text, "atta|aata", "आटा", ignoreCase = true)
        text = replaceWord(text, "ghee", "घी", ignoreCase = true)
        text = replaceWord(text, "doodh|milk", "दूध", ignoreCase = true)
        text = replaceWord(text, "namak|salt", "नमक", ignoreCase = true)
        text = replaceWord(text, "masala", "मसाला", ignoreCase = true)
        text = replaceWord(text, "बिस्किट|biscuit", "बिस्कुट", ignoreCase = true)
        text = replaceWord(text, "colgate|टूथपेस्ट", "कोलगेट", ignoreCase = true)
        text = replaceWord(text, "माचीस|matchbox", "माचिस", ignoreCase = true)

        // 8. Standardize common Hindi number spellings
        text = replaceWord(text, "पांच", "पाँच")
        text = replaceWord(text, "छः", "छह")

        // 9. Clean spacing between digits and Devanagari text (e.g. "10रुपये" -> "10 रुपये", "2किलो" -> "2 किलो")
        text = text.replace(Regex("(\\d+)([\\u0900-\\u097F]+)"), "$1 $2")
        text = text.replace(Regex("([\\u0900-\\u097F]+)(\\d+)"), "$1 $2")

        // 10. Final cleanup of multiple spaces
        text = text.replace(Regex("\\s+"), " ").trim()

        return CorrectionResult(
            original = rawInput,
            corrected = text,
            isModified = (text != rawInput.trim())
        )
    }
}
