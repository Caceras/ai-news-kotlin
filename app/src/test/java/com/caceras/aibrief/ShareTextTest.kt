package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.PostBlock
import com.caceras.aibrief.data.StoryFormat
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {
    private val story = NewsArticle(id = "a", title = "A model ships", source = "Example",
        url = "https://example.com/a", publishedAt = Instant.parse("2026-11-05T08:00:00Z"),
        summary = "Summary", category = NewsCategory.entries.first())

    @Test
    fun sharesHeadlineSummaryAndLink() {
        assertEquals("A model ships\n\nSummary\n\nhttps://example.com/a", shareText(story))
    }

    @Test
    fun leavesOutABlankSummaryWithoutEmptyLines() {
        assertEquals("A model ships\n\nhttps://example.com/a", shareText(story.copy(summary = "  ")))
    }

    @Test
    fun sponsoredStoriesKeepTheirDisclosure() {
        val paid = story.copy(url = "", format = StoryFormat.SPONSORED, sponsor = "Acme",
            blocks = listOf(PostBlock.Paragraph("Body")))
        val text = shareText(paid)
        assertTrue(text.startsWith("A model ships\n\nSponsored by Acme\n\n"))
        assertTrue(text.endsWith("From AI Brief"))
    }

    @Test
    fun originalPostsListEachSourceOnce() {
        val post = story.copy(url = "", blocks = listOf(
            PostBlock.Paragraph("Body"),
            PostBlock.Link("Paper", "https://example.com/paper"),
            PostBlock.Link("Paper again", "https://example.com/paper"),
            PostBlock.Link("", "https://example.com/blog"),
        ))
        val text = shareText(post)
        assertTrue(text.contains("Paper: https://example.com/paper"))
        assertFalse(text.contains("Paper again"))
        assertTrue(text.contains("\n\nhttps://example.com/blog\n\n"))
        assertFalse(text.contains("\n\n\n"))
    }

    @Test
    fun newsStoriesAreNotSignedAsAiBrief() {
        assertFalse(shareText(story).contains("From AI Brief"))
    }
}
