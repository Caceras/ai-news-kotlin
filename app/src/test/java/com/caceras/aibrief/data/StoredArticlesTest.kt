package com.caceras.aibrief.data

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredArticlesTest {
    private fun article(id: String) = NewsArticle(
        id = id,
        title = "Title $id",
        source = "AI Brief",
        author = "Editor",
        url = "",
        imageUrl = "https://example.com/$id.jpg",
        publishedAt = Instant.parse("2026-11-01T00:00:00Z"),
        summary = "Summary $id",
        category = NewsCategory.POLICY,
        blocks = listOf(
            PostBlock.Paragraph("Body $id"),
            PostBlock.Link("Source", "https://example.com/source"),
        ),
    )

    @Test
    fun aSavedShelfReadsBackExactly() {
        val shelf = listOf(article("a"), article("b"))
        assertEquals(shelf, StoredArticles.decode(StoredArticles.encode(shelf, 80)))
    }

    @Test
    fun encodingKeepsAtMostTheLimit() {
        val shelf = (1..5).map { article("p$it") }
        assertEquals(listOf("p1", "p2"), StoredArticles.decode(StoredArticles.encode(shelf, 2)).map { it.id })
    }

    @Test
    fun oneUnreadableEntryDoesNotEraseTheRestOfTheShelf() {
        val stored = JSONArray(StoredArticles.encode(listOf(article("a"), article("b")), 80))
        stored.put(JSONObject().put("title", "No id"))
        stored.put("not an object")
        stored.put(JSONObject(stored.getJSONObject(0).toString()).put("id", "c").put("publishedAt", "soon"))
        assertEquals(listOf("a", "b"), StoredArticles.decode(stored.toString()).map { it.id })
    }

    @Test
    fun aCategoryFromANewerBuildFallsBackInsteadOfDroppingThePost() {
        val stored = JSONArray(StoredArticles.encode(listOf(article("a")), 80))
        stored.getJSONObject(0).put("category", "HARDWARE")
        val restored = StoredArticles.decode(stored.toString()).single()
        assertEquals(NewsCategory.ALL, restored.category)
        assertEquals("Title a", restored.title)
    }

    @Test
    fun missingOrCorruptStorageIsAnEmptyShelf() {
        assertTrue(StoredArticles.decode(null).isEmpty())
        assertTrue(StoredArticles.decode("").isEmpty())
        assertTrue(StoredArticles.decode("{broken").isEmpty())
    }
}
