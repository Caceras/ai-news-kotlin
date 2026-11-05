package com.caceras.aibrief.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryFormatTest {
    private fun doc(fields: String) = """{"version":1,"posts":[{"id":"a","title":"T",
        "publishedAt":"2026-10-01T10:00:00Z","summary":"S","author":"A",
        "blocks":[{"type":"paragraph","text":"Body"},{"type":"link","text":"Src","url":"https://example.com"}]
        $fields}]}"""

    @Test
    fun `a post without a format, or with one this build does not know, reads as news`() {
        assertEquals(StoryFormat.NEWS, OwnerPosts.parse(doc("")).single().format)
        assertEquals(StoryFormat.NEWS, OwnerPosts.parse(doc(""","format":"hologram"""")).single().format)
        assertEquals(StoryFormat.OPINION, OwnerPosts.parse(doc(""","format":"opinion"""")).single().format)
    }

    @Test
    fun `sponsored posts that do not name their sponsor are never shown`() {
        assertTrue(OwnerPosts.parse(doc(""","format":"SPONSORED"""")).isEmpty())
        val post = OwnerPosts.parse(doc(""","format":"SPONSORED","sponsor":"Acme"""")).single()
        assertEquals("Acme", post.sponsor)
    }

    @Test
    fun `breaking collages keep at most two extra https pictures`() {
        val post = OwnerPosts.parse(doc(""","format":"BREAKING","gallery":["http://insecure.example/a.jpg",
            "https://e.com/1.jpg","https://e.com/1.jpg","https://e.com/2.jpg","https://e.com/3.jpg"]""")).single()
        assertEquals(listOf("https://e.com/1.jpg", "https://e.com/2.jpg"), post.gallery)
    }

    @Test
    fun `an update time is kept only when it follows publication, and long kickers are dropped`() {
        val post = OwnerPosts.parse(doc(""","updatedAt":"2026-09-01T00:00:00Z",
            "kicker":"This kicker is far too long to sit before a headline"""")).single()
        assertNull(post.updatedAt)
        assertNull(post.kicker)
        val updated = OwnerPosts.parse(doc(""","updatedAt":"2026-10-02T08:00:00Z","kicker":"Revealed"""")).single()
        assertEquals(Instant.parse("2026-10-02T08:00:00Z"), updated.updatedAt)
        assertEquals("Revealed", updated.kicker)
    }

    @Test
    fun `card themes are named or picked stably from the id`() {
        assertEquals(CardTheme.MINT, CardTheme.parse("mint"))
        assertNull(CardTheme.parse("neon"))
        assertEquals(CardTheme.forId("post-x"), CardTheme.forId("post-x"))
    }

    @Test
    fun `saved stories keep their format and presentation fields`() {
        val article = NewsArticle(id = "post-a", title = "T", source = "AI Brief", url = "",
            imageUrl = "https://e.com/c.jpg", publishedAt = Instant.parse("2026-10-01T10:00:00Z"),
            summary = "S", category = NewsCategory.POLICY, blocks = listOf(PostBlock.Paragraph("Body")),
            format = StoryFormat.SPONSORED, kicker = "Now", gallery = listOf("https://e.com/g.jpg"),
            authorImageUrl = "https://e.com/p.jpg", authorRole = "Columnist", theme = CardTheme.LILAC,
            sponsor = "Acme", updatedAt = Instant.parse("2026-10-02T10:00:00Z"))
        val restored = StoredArticles.decode(StoredArticles.encode(listOf(article), 10)).single()
        assertEquals(article, restored)
    }
}
