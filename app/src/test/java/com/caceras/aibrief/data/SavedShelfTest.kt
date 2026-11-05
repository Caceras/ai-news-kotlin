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

    @Test
    fun undoingARemovalPutsThePostBackWhereItWas() {
        val shelf = restoredSaved(listOf(article("a"), article("c")), article("b"), index = 1)
        assertEquals(listOf("a", "b", "c"), shelf.map { it.id })
    }

    @Test
    fun undoingAtAnIndexPastTheEndAppends() {
        val shelf = restoredSaved(listOf(article("a")), article("b"), index = 5)
        assertEquals(listOf("a", "b"), shelf.map { it.id })
    }

    @Test
    fun undoingTwiceDoesNotDuplicateThePost() {
        val shelf = restoredSaved(listOf(article("a"), article("b")), article("b"), index = 0)
        assertEquals(listOf("a", "b"), shelf.map { it.id })
    }
}
