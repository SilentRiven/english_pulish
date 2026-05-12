package com.workplat.englishpulish.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextParserTest {

    @Test
    fun `single lowercase word`() {
        assertEquals(ParseResult.Word("abandon"), TextParser.parse("abandon"))
    }

    @Test
    fun `word with surrounding whitespace and punctuation`() {
        assertEquals(ParseResult.Word("abandon"), TextParser.parse("  abandon.  "))
    }

    @Test
    fun `capitalized word is lowercased`() {
        assertEquals(ParseResult.Word("ability"), TextParser.parse("Ability"))
    }

    @Test
    fun `hyphenated and apostrophe tokens are preserved`() {
        assertEquals(ParseResult.Word("won't"), TextParser.parse("won't"))
        assertEquals(ParseResult.Word("state-of-the-art"), TextParser.parse("state-of-the-art"))
    }

    @Test
    fun `single letter is dropped as too short`() {
        assertEquals(ParseResult.Empty, TextParser.parse("a"))
        assertEquals(ParseResult.Empty, TextParser.parse("I"))
    }

    @Test
    fun `multi token sentence becomes Sentence`() {
        val result = TextParser.parse("She abandoned the plan after careful consideration.")
        assertEquals(
            ParseResult.Sentence(
                listOf("she", "abandoned", "the", "plan", "after", "careful", "consideration")
            ),
            result,
        )
    }

    @Test
    fun `duplicate tokens are deduped preserving first-seen order`() {
        val result = TextParser.parse("the cat saw the dog")
        assertEquals(
            ParseResult.Sentence(listOf("the", "cat", "saw", "dog")),
            result,
        )
    }

    @Test
    fun `empty and whitespace-only inputs`() {
        assertEquals(ParseResult.Empty, TextParser.parse(""))
        assertEquals(ParseResult.Empty, TextParser.parse("   "))
        assertEquals(ParseResult.Empty, TextParser.parse("..."))
    }

    @Test
    fun `non latin text yields Empty`() {
        assertEquals(ParseResult.Empty, TextParser.parse("放弃 这个 计划"))
    }

    @Test
    fun `mixed Chinese English keeps only English tokens`() {
        assertEquals(ParseResult.Word("abandon"), TextParser.parse("放弃 = abandon"))
    }
}
