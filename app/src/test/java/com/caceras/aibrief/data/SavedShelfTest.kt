package com.caceras.aibrief.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedShelfTest {
    private fun article(id: String) = NewsArticle(
        id = id,
        title = "Title $id",
        source = "AI Brief",
        url = "https://example.com/$id",
        publishedAt = Instant.parse("2026-11-01T00:00:00Z"),
        summary = "Summary $id",
        category = NewsCategory.ALL,
    )

    @Test
    fun savingPutsTheNewestPostOnTop() {
        val shelf = toggledSaved(listOf(article("a")), article("b"))
        assertEquals(listOf("b", "a"), shelf.map { it.id })
    }

    @Test
    fun savingAgainRemovesThePost() {
        val shelf = toggledSaved(listOf(article("a"), article("b")), article("a"))
        assertEquals(listOf("b"), shelf.map { it.id })
    }

    @Test
    fun aFullShelfDropsTheOldestSave() {
        val full = (1..MAX_SAVED_ARTICLES).map { article("$it") }
        val shelf = toggledSaved(full, article("new"))
        assertEquals(MAX_SAVED_ARTICLES, shelf.size)
        assertEquals("new", shelf.first().id)
        assertEquals("${MAX_SAVED_ARTICLES - 1}", shelf.last().id)
    }
}
