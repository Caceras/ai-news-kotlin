package com.caceras.aibrief.ui.components

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BylineTest {
    private val utc = ZoneId.of("UTC")
    private val published = Instant.parse("2026-03-04T09:00:00Z")

    private fun story(updatedAt: Instant?) = NewsArticle(
        id = "a", title = "Title", source = "AI Brief", author = "Ada", url = "",
        publishedAt = published, summary = "Summary", category = NewsCategory.ALL, updatedAt = updatedAt,
    )

    @Test
    fun timesFollowTheTwentyFourHourClock() {
        val at = Instant.parse("2026-03-05T14:05:00Z")
        assertEquals("Mar 5, 14:05", at.readerTime(use24Hour = true, zone = utc))
        assertEquals("Mar 5, 2:05 PM", at.readerTime(use24Hour = false, zone = utc))
    }

    @Test
    fun anUpdateIsNamedWithTheReadersClock() {
        val byline = story(Instant.parse("2026-03-05T14:05:00Z")).bylineText(use24Hour = false, zone = utc)
        assertEquals(true, byline.startsWith("By Ada · March 4, 2026 · Updated Mar 5, 2:05 PM · "))
    }

    @Test
    fun anUpdateNoLaterThanPublicationIsLeftOut() {
        assertEquals(false, story(published).bylineText(zone = utc).contains("Updated"))
        assertEquals(false, story(published.minusSeconds(60)).bylineText(zone = utc).contains("Updated"))
        assertEquals(false, story(null).bylineText(zone = utc).contains("Updated"))
    }
}
