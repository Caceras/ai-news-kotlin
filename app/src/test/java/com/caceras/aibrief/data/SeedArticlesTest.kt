package com.caceras.aibrief.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedArticlesTest {
    @Test
    fun offlineEditionContainsDistinctReadableArticles() {
        val articles = SeedArticles.create(Instant.parse("2026-08-16T12:00:00Z"))

        assertTrue(articles.size >= 6)
        assertEquals(articles.size, articles.map { it.id }.distinct().size)
        assertTrue(articles.all { it.url.startsWith("https://") })
        assertTrue(articles.all { it.title.isNotBlank() && it.summary.isNotBlank() })
        assertTrue(articles.all { it.isOfflineBrief && it.source == "AI Brief" })
    }
}
