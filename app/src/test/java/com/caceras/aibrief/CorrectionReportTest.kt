package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionReportTest {
    private val story = NewsArticle(id = "gpt-launch", title = "A model ships", source = "AI Brief",
        url = "https://example.com/a", publishedAt = Instant.parse("2026-11-05T08:00:00Z"),
        summary = "Summary", category = NewsCategory.entries.first())

    @Test
    fun namesTheStoryTheCopyAndTheAppVersion() {
        val email = correctionEmail(story.copy(updatedAt = Instant.parse("2026-11-05T12:00:00Z")), "1.2 (build 49)")

        assertEquals("Correction: A model ships", email.subject)
        assertTrue(email.body.contains("Story ID: gpt-launch"))
        assertTrue(email.body.contains("Link: https://example.com/a"))
        assertTrue(email.body.contains("Updated: 2026-11-05T12:00:00Z"))
        assertTrue(email.body.contains("App: AI Brief 1.2 (build 49)"))
    }

    @Test
    fun leavesOutLinesThatHaveNoValue() {
        val email = correctionEmail(story.copy(url = ""), "1.2")

        assertFalse(email.body.contains("Link:"))
        assertFalse(email.body.contains("Updated:"))
    }
}
