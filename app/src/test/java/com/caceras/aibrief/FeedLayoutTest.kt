package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.SeriesInfo
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
            is FeedSlot.SeriesRail -> slot.parts
            is FeedSlot.Ribbon, FeedSlot.Rankings, FeedSlot.Radar -> emptyList()
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
    fun `the front page reads lead, pair, latest four, designed card, modules, opinion, then more`() {
        val input = (1..10).map { story("n$it", it.toLong()) } +
            (1..2).map { story("o$it", it.toLong(), StoryFormat.OPINION) } +
            story("g1", 3, StoryFormat.GUIDE)
        val slots = FeedLayout.compose(input, now, FeedModules(rankings = true, radar = true))
        assertEquals(listOf("Lead", "Pair", "Ribbon", "Row", "Row", "Row", "Row", "Designed", "Rankings", "Radar",
            "Ribbon", "OpinionRail", "Ribbon", "Headline", "Pair"), slots.map { it::class.simpleName })
        assertEquals("Latest", (slots[2] as FeedSlot.Ribbon).title)
        // "Latest" is newest first.
        val latest = slots.filterIsInstance<FeedSlot.Row>().take(4).map { it.article.publishedAt }
        assertEquals(latest.sortedDescending(), latest)
    }

    @Test
    fun `data modules appear only when the edition carries them`() {
        val slots = FeedLayout.compose((1..4).map { story("n$it", it.toLong()) }, now)
        assertFalse(slots.any { it == FeedSlot.Rankings || it == FeedSlot.Radar })
    }

    @Test
    fun `a series is gathered in order into one rail`() {
        fun part(n: Int, hoursAgo: Long) = story("s$n", hoursAgo, StoryFormat.EXPLAINER)
            .copy(series = SeriesInfo("ai-and-philosophy", "AI and philosophy", n, 3))
        val parts = listOf(part(1, 30), part(2, 20), part(3, 10))
        val slots = FeedLayout.compose(parts + (1..6).map { story("n$it", it.toLong()) }, now)
        val rail = slots.filterIsInstance<FeedSlot.SeriesRail>().single()
        assertEquals(listOf("s1", "s2", "s3"), rail.parts.map { it.id })
        assertTrue(slots.any { it is FeedSlot.Ribbon && it.title == "Series: AI and philosophy" })
        // Parts appear only in their rail.
        assertEquals(3, articlesOf(slots).count { it.series != null })
    }

    @Test
    fun `nothing designed sits directly under a lead with no news after it`() {
        val slots = FeedLayout.compose(listOf(story("n1", 1), story("ad", 2, StoryFormat.SPONSORED)), now)
        assertFalse(slots[1] is FeedSlot.Designed)
    }

    @Test
    fun `breaking news reads as news a day after it broke`() {
        val stale = story("old", 30, StoryFormat.BREAKING)
        assertFalse(stale.isBreakingAt(now))
        assertEquals(StoryFormat.NEWS, stale.asOf(now).format)
        val fresh = story("new", 3, StoryFormat.BREAKING)
        assertTrue(fresh.isBreakingAt(now))
        assertEquals(fresh, fresh.asOf(now))
    }

    @Test
    fun `an update keeps a story breaking and lets it lead`() {
        val updated = story("updated", 30, StoryFormat.BREAKING).copy(updatedAt = now.minus(Duration.ofHours(2)))
        assertTrue(updated.isBreakingAt(now))
        val lead = FeedLayout.compose(listOf(story("n1", 1), updated), now).first() as FeedSlot.Lead
        assertEquals("updated", lead.article.id)
    }

    @Test
    fun `other formats are never breaking`() {
        val opinion = story("op", 1, StoryFormat.OPINION)
        assertFalse(opinion.isBreakingAt(now))
        assertEquals(opinion, opinion.asOf(now))
    }
}
