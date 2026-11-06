package com.caceras.aibrief

import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.SeriesInfo
import com.caceras.aibrief.data.StoryFormat
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpNextTest {
    private fun story(id: String, format: StoryFormat = StoryFormat.NEWS, series: SeriesInfo? = null) = NewsArticle(
        id = id, title = id, source = "AI Brief", url = "", publishedAt = Instant.parse("2026-11-05T12:00:00Z"),
        summary = "", category = NewsCategory.ALL, format = format, series = series,
        sponsor = "Acme".takeIf { format == StoryFormat.SPONSORED })

    private val a = story("a")
    private val b = story("b")
    private val c = story("c")

    @Test
    fun `offers the next story in the edition`() {
        assertEquals("b", UpNext.after(a, listOf(a, b, c))?.id)
    }

    @Test
    fun `wraps round from the last story`() {
        assertEquals("a", UpNext.after(c, listOf(a, b, c))?.id)
    }

    @Test
    fun `a story missing from the edition offers the newest`() {
        assertEquals("a", UpNext.after(story("saved"), listOf(a, b, c))?.id)
    }

    @Test
    fun `never offers sponsored content`() {
        val paid = story("paid", StoryFormat.SPONSORED)
        assertEquals("c", UpNext.after(a, listOf(a, paid, c))?.id)
    }

    @Test
    fun `skips other parts of the same series`() {
        val one = story("one", series = SeriesInfo("art", "AI & Art", 1, 2))
        val two = story("two", series = SeriesInfo("art", "AI & Art", 2, 2))
        assertEquals("c", UpNext.after(one, listOf(one, two, c))?.id)
        assertEquals("one", UpNext.after(a, listOf(a, one, two))?.id)
    }

    @Test
    fun `skips stories on the reader's trail`() {
        assertEquals("c", UpNext.after(a, listOf(a, b, c), trailIds = setOf("b"))?.id)
    }

    @Test
    fun `nothing left offers nothing`() {
        assertNull(UpNext.after(a, listOf(a)))
        assertNull(UpNext.after(a, listOf(a, b), setOf("b")))
    }
}
