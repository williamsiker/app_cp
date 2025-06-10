package com.example.lancelot.snippets

import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetManagerTest {
    @Test
    fun suggestionsMatchPrefix() {
        val suggestions = SnippetManager.getSuggestions("cpp", "fo")
        assertTrue(suggestions.any { it.prefix == "for" })
    }

    @Test
    fun unknownLanguageReturnsEmpty() {
        val suggestions = SnippetManager.getSuggestions("unknown", "for")
        assertTrue(suggestions.isEmpty())
    }
}
