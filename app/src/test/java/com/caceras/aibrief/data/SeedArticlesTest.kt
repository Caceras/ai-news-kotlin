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
        assertTrue(articles.all { it.author == "AI Brief" && it.imageUrl == null })
        assertTrue(articles.all { it.readingTimeMinutes in 1..10 })
    }

    @Test
    fun readingTimeCalculationHandlesVariousLengths() {
        val shortArticle = NewsArticle(
            id = "test-1",
            title = "Short Title",
            source = "Test",
            url = "https://example.com/1",
            publishedAt = Instant.now(),
            summary = "Brief summary of only a few words.",
            category = NewsCategory.ALL,
        )
        assertEquals(1, shortArticle.readingTimeMinutes)

        val longSummary = (1..200).joinToString(" ") { "word$it" }
        val longArticle = NewsArticle(
            id = "test-2",
            title = "A Comprehensive Overview of Neural Architectures",
            source = "Test",
            url = "https://example.com/2",
            publishedAt = Instant.now(),
            summary = longSummary,
            category = NewsCategory.RESEARCH,
        )
        assertTrue(longArticle.readingTimeMinutes >= 4)
    }

    @Test
    fun allCategoriesHaveEnumLabels() {
        NewsCategory.entries.forEach { category ->
            assertTrue(category.label.isNotBlank())
        }
    }
}
