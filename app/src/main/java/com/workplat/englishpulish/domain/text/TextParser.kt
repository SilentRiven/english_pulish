package com.workplat.englishpulish.domain.text

/**
 * Result of parsing shared text into one or more candidate lemmas.
 *
 * - [Word]: a single token. Goes straight into the library.
 * - [Sentence]: multiple tokens. UI should let the user pick which ones to add.
 * - [Empty]: nothing usable (empty / pure punctuation / non-English).
 */
sealed interface ParseResult {
    data class Word(val lemma: String) : ParseResult
    data class Sentence(val tokens: List<String>) : ParseResult
    data object Empty : ParseResult
}

object TextParser {

    private val tokenRegex = Regex("[a-zA-Z][a-zA-Z'\\-]*")

    fun parse(raw: String): ParseResult {
        val tokens = tokenRegex.findAll(raw)
            .map { it.value }
            .map(::normalize)
            .filter { it.length >= 2 }
            .toList()

        return when (tokens.size) {
            0 -> ParseResult.Empty
            1 -> ParseResult.Word(tokens.first())
            else -> ParseResult.Sentence(tokens.distinct())
        }
    }

    fun normalize(token: String): String =
        token.trim().lowercase().trim('\'', '-')
}
