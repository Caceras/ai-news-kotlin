package com.caceras.aibrief

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ReadHistoryTest {
    @Test
    fun `marks a story as the latest read`() {
        assertEquals(listOf("a", "b"), ReadHistory.marked(setOf("a"), "b").toList())
    }

    @Test
    fun `marking a read story again changes nothing`() {
        val ids = setOf("a", "b")
        assertSame(ids, ReadHistory.marked(ids, "a"))
    }

    @Test
    fun `keeps only the latest stories`() {
        var ids: Set<String> = emptySet()
        repeat(ReadHistory.MAX_READ + 5) { ids = ReadHistory.marked(ids, "s$it") }
        assertEquals(ReadHistory.MAX_READ, ids.size)
        assertEquals("s5", ids.first())
        assertEquals("s${ReadHistory.MAX_READ + 4}", ids.last())
    }

    @Test
    fun `survives a round trip and ignores blank or missing storage`() {
        val ids = setOf("story-1", "story-2")
        assertEquals(ids.toList(), ReadHistory.decode(ReadHistory.encode(ids)).toList())
        assertEquals(emptySet<String>(), ReadHistory.decode(null))
        assertEquals(setOf("x"), ReadHistory.decode("\n x \n\n"))
    }
}
