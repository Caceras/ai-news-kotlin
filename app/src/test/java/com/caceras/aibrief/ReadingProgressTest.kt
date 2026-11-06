package com.caceras.aibrief

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingProgressTest {
    @Test
    fun emptyWhileTheTextStartsBelowTheHeader() {
        assertEquals(0f, readingProgress(proseOffset = 600, proseSize = 5000, viewport = 2000), 0f)
        assertEquals(0f, readingProgress(proseOffset = 0, proseSize = 5000, viewport = 2000), 0f)
    }

    @Test
    fun fillsInProportionToTheTextScrolledPast() {
        assertEquals(0.5f, readingProgress(proseOffset = -1500, proseSize = 5000, viewport = 2000), 0.001f)
    }

    @Test
    fun fullWhenTheLastLineReachesTheBottomAndAfter() {
        assertEquals(1f, readingProgress(proseOffset = -3000, proseSize = 5000, viewport = 2000), 0f)
        assertEquals(1f, readingProgress(proseOffset = -4800, proseSize = 5000, viewport = 2000), 0f)
    }

    @Test
    fun shortStoryCountsAsReadOnceAllOfItIsInView() {
        assertEquals(0f, readingProgress(proseOffset = 1800, proseSize = 900, viewport = 2000), 0f)
        assertEquals(1f, readingProgress(proseOffset = 900, proseSize = 900, viewport = 2000), 0f)
    }
}
