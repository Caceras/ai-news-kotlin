package com.caceras.aibrief

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTextSizeTest {
    @Test
    fun eachTapMakesTextLargerThenReturnsToStandard() {
        assertEquals(ReaderTextSize.LARGE, ReaderTextSize.STANDARD.next())
        assertEquals(ReaderTextSize.LARGEST, ReaderTextSize.LARGE.next())
        assertEquals(ReaderTextSize.STANDARD, ReaderTextSize.LARGEST.next())
    }

    @Test
    fun sizesGrowAndStandardLeavesSystemTextAlone() {
        assertEquals(1f, ReaderTextSize.STANDARD.scale)
        ReaderTextSize.entries.zipWithNext().forEach { (smaller, larger) -> assertTrue(larger.scale > smaller.scale) }
    }

    @Test
    fun storedNamesRoundTripAndAnythingElseIsStandard() {
        ReaderTextSize.entries.forEach { assertEquals(it, ReaderTextSize.decode(it.name)) }
        assertEquals(ReaderTextSize.STANDARD, ReaderTextSize.decode(null))
        assertEquals(ReaderTextSize.STANDARD, ReaderTextSize.decode("HUGE"))
    }
}
