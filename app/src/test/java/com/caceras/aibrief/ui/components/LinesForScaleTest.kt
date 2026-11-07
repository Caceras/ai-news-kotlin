package com.caceras.aibrief.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LinesForScaleTest {
    @Test fun defaultTextKeepsTheDesignedCap() {
        assertEquals(4, linesForScale(4, 1f))
        assertEquals(4, linesForScale(4, 0.85f))
    }

    @Test fun largeTextRaisesTheCapSoWordsAreNotCutOff() {
        assertEquals(8, linesForScale(4, 2f))
        assertEquals(5, linesForScale(4, 1.15f))
        assertEquals(2, linesForScale(1, 1.3f))
    }

    @Test fun uncappedTextStaysUncapped() {
        assertEquals(Int.MAX_VALUE, linesForScale(Int.MAX_VALUE, 2f))
    }
}
