package com.caceras.aibrief

import org.junit.Assert.assertEquals
import org.junit.Test

class MergedReadIdsTest {
    @Test
    fun `adds stories opened while the stored list loaded`() {
        assertEquals(listOf("a", "b", "c"), mergedReadIds(setOf("a", "b"), setOf("c"), cleared = false).toList())
    }

    @Test
    fun `keeps the stored list when nothing was opened`() {
        assertEquals(setOf("a"), mergedReadIds(setOf("a"), emptySet(), cleared = false))
    }

    @Test
    fun `a history cleared while loading drops the stored list`() {
        assertEquals(emptySet<String>(), mergedReadIds(setOf("a", "b"), emptySet(), cleared = true))
    }

    @Test
    fun `stories opened after clearing stay read`() {
        assertEquals(setOf("c"), mergedReadIds(setOf("a"), setOf("c"), cleared = true))
    }

    @Test
    fun `undoing a clear brings the list back and keeps stories opened since`() {
        assertEquals(listOf("a", "b", "c"), mergedReadIds(setOf("a", "b"), setOf("c"), cleared = false).toList())
        assertEquals(listOf("a", "b"), mergedReadIds(setOf("a", "b"), setOf("b"), cleared = false).toList())
    }
}
