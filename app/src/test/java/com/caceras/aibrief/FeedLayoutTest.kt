package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.StoryFormat
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedLayoutTest {
    private val now = Instant.parse("2026-11-05T12:00:00Z")

    private fun story(id: String, hoursAgo: Long, format: StoryFormat = StoryFormat.NEWS) = NewsArticle(
        id = id, title = id, source = "AI Brief", url = "", publishedAt = now.minus(Duration.ofHours(hoursAgo)),
        summary = "", category = NewsCategory.ALL, format = format, sponsor = "Acme".takeIf {
            format == StoryFormat.SPONSORED })

    private fun articlesOf(slots: List<FeedSlot>) = slots.flatMap { slot ->
        when (slot) {
            is FeedSlot.Lead -> listOf(slot.article)
            is FeedSlot.Headline -> listOf(slot.article)
            is FeedSlot.Pair -> listOf(slot.left, slot.right)
            is FeedSlot.Row -> listOf(slot.article)
            is FeedSlot.OpinionRail -> slot.articles
            is FeedSlot.Designed -> listOf(slot.article)
            is FeedSlot.Ribbon -> emptyList()
        }
    }

    @Test
    fun `an empty edition has no slots`() {
        assertTrue(FeedLayout.compose(emptyList(), now).isEmpty())
    }

    @Test
    fun `fresh breaking news leads, stale breaking news does not`() {
        val fresh = story("breaking", 30, StoryFormat.BREAKING)
        val newer = story("news", 1)
        assertEquals(newer, (FeedLayout.compose(listOf(newer, fresh), now).first() as FeedSlot.Lead).article)
        val recent = story("breaking", 3, StoryFormat.BREAKING)
        assertEquals(recent, (FeedLayout.compose(listOf(newer, recent), now).first() as FeedSlot.Lead).article)
    }

    @Test
    fun `guides, sponsored posts and columns never lead while there is news`() {
        val slots = FeedLayout.compose(listOf(story("guide", 0, StoryFormat.GUIDE),
            story("ad", 0, StoryFormat.SPONSORED), story("column", 0, StoryFormat.OPINION), story("news", 5)), now)
        assertEquals("news", (slots.first() as FeedSlot.Lead).article.id)
    }

    @Test
    fun `every story appears exactly once`() {
        val input = (1..17).map { story("n$it", it.toLong()) } +
            (1..3).map { story("o$it", it.toLong(), StoryFormat.OPINION) } +
            (1..4).map { story("g$it", it.toLong(), if (it % 2 == 0) StoryFormat.SPONSORED else StoryFormat.GUIDE) }
        val placed = articlesOf(FeedLayout.compose(input, now))
        assertEquals(input.size, placed.size)
        assertEquals(input.toSet(), placed.toSet())
    }

    @Test
    fun `designed cards never touch each other or the lead`() {
        val input = (1..9).map { story("n$it", it.toLong()) } +
            (1..5).map { story("g$it", it.toLong(), StoryFormat.SPONSORED) }
        val slots = FeedLayout.compose(input, now)
        slots.zipWithNext().forEach { (a, b) ->
            assertFalse("Two designed cards touch: $a $b", a is FeedSlot.Designed && b is FeedSlot.Designed &&
                slots.indexOf(a) < slots.indexOfFirst { it is FeedSlot.Ribbon && it.title == "Guides" })
            assertFalse(a is FeedSlot.Lead && b is FeedSlot.Designed)
        }
    }

    @Test
    fun `news follows the rhythm and columns gather in one rail`() {
        val input = (1..8).map { story("n$it", it.toLong()) } +
            (1..2).map { story("o$it", it.toLong(), StoryFormat.OPINION) }
        val kinds = FeedLayout.compose(input, now).map { it::class.simpleName }
        assertEquals(listOf("Lead", "Ribbon", "Headline", "Pair", "Row", "Row", "Ribbon", "OpinionRail",
            "Headline", "Row"), kinds)
    }
}
