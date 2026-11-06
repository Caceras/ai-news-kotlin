package com.caceras.aibrief

import androidx.activity.BackEventCompat
import com.caceras.aibrief.ui.theme.Motion
import org.junit.Assert.assertEquals
import org.junit.Test

class BackPreviewTest {
    @Test
    fun storyShrinksWithTheSwipeAndNoFurther() {
        assertEquals(1f, BackPreview.scale(0f), 0.0001f)
        assertEquals(0.95f, BackPreview.scale(0.5f), 0.0001f)
        assertEquals(Motion.backPreviewScale, BackPreview.scale(1f), 0.0001f)
        assertEquals(Motion.backPreviewScale, BackPreview.scale(1.4f), 0.0001f)
        assertEquals(1f, BackPreview.scale(-0.2f), 0.0001f)
    }

    @Test
    fun storySlidesAwayFromTheSwipedEdge() {
        assertEquals(1f, BackPreview.direction(BackEventCompat.EDGE_LEFT), 0f)
        assertEquals(-1f, BackPreview.direction(BackEventCompat.EDGE_RIGHT), 0f)
        // A back button press or a swipe from no edge (EDGE_NONE in newer releases).
        assertEquals(0f, BackPreview.direction(2), 0f)
    }
}
