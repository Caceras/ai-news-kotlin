package com.caceras.aibrief

import com.caceras.aibrief.data.FeedFreshness
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class EditionLabelTest {
    private val now = Instant.parse("2026-11-05T12:00:00Z")

    private fun cachedAgo(age: Duration) = editionLabel(FeedFreshness.CACHED, now.minus(age), now)

    @Test
    fun liveAndOfflineKeepTheirLabels() {
        assertEquals("Latest edition", editionLabel(FeedFreshness.LIVE, now.minusSeconds(9_999), now))
        assertEquals("Offline edition", editionLabel(FeedFreshness.OFFLINE, null, now))
    }

    @Test
    fun aCachedCopySaysHowOldItIs() {
        assertEquals("Updated just now", cachedAgo(Duration.ofSeconds(30)))
        assertEquals("Updated 1 min ago", cachedAgo(Duration.ofMinutes(1)))
        assertEquals("Updated 59 min ago", cachedAgo(Duration.ofMinutes(59)))
        assertEquals("Updated 1 h ago", cachedAgo(Duration.ofMinutes(60)))
        assertEquals("Updated 23 h ago", cachedAgo(Duration.ofHours(23).plusMinutes(59)))
        assertEquals("Updated yesterday", cachedAgo(Duration.ofHours(24)))
        assertEquals("Updated 3 days ago", cachedAgo(Duration.ofDays(3)))
    }

    @Test
    fun anUnknownOrFutureTimeFallsBackToThePlainLabel() {
        assertEquals("Cached edition", editionLabel(FeedFreshness.CACHED, null, now))
        assertEquals("Cached edition", editionLabel(FeedFreshness.CACHED, now.plusSeconds(600), now))
    }
}
