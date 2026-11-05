package com.caceras.aibrief.ui.components

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeSizeTest {
    @Test
    fun anUnmeasuredBoxDecodesNothing() {
        assertNull(DecodeSize.forBox(IntSize.Zero))
        assertNull(DecodeSize.forBox(IntSize(300, 0)))
    }

    @Test
    fun boxesRoundUpToAStepSoNearbySizesShareACopy() {
        assertEquals(DecodeSize(384, 256), DecodeSize.forBox(IntSize(280, 210)))
        assertEquals(DecodeSize(384, 256), DecodeSize.forBox(IntSize(300, 240)))
    }

    @Test
    fun noBoxAsksForMoreThanThePublishingLimit() {
        assertEquals(DecodeSize(1600, 1600), DecodeSize.forBox(IntSize(2400, 1800)))
    }

    @Test
    fun aLargerDecodeCoversASmallerBox() {
        assertTrue(DecodeSize(1152, 768).covers(DecodeSize(384, 256)))
        assertFalse(DecodeSize(384, 256).covers(DecodeSize(1152, 768)))
        assertFalse(DecodeSize(1152, 256).covers(DecodeSize(384, 512)))
    }

    @Test
    fun aWideCoverInAThumbnailIsDecodedToFillItsHeight() {
        // 1600 x 1000 cropped into a 384 x 256 box: height decides, 256 / 1000.
        assertEquals(410, DecodeSize.targetWidth(1600, 1000, DecodeSize(384, 256)))
    }

    @Test
    fun aFullWidthHeroIsDecodedToTheScreenNotThePublishedSize() {
        assertEquals(1152, DecodeSize.targetWidth(1600, 1000, DecodeSize(1152, 640)))
    }

    @Test
    fun aSmallPictureIsNeverScaledUp() {
        assertEquals(640, DecodeSize.targetWidth(640, 640, DecodeSize(1152, 768)))
    }

    @Test
    fun samplingStopsBeforeThePictureGetsSmallerThanTheTarget() {
        assertEquals(1, ArticleImageCache.sampleSize(1600, 1152))
        assertEquals(2, ArticleImageCache.sampleSize(1600, 800))
        assertEquals(2, ArticleImageCache.sampleSize(1600, 410))
        assertEquals(4, ArticleImageCache.sampleSize(1600, 400))
    }
}
