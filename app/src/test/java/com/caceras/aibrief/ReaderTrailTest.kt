package com.caceras.aibrief

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTrailTest {
    @Test
    fun backWalksTheTrailNewestFirstThenReachesTheFrontPage() {
        val trail = ReaderTrail().push(TrailStop("part-1", 3, 40)).push(TrailStop("part-2", 1, 0))

        val (first, afterFirst) = trail.pop()
        assertEquals(TrailStop("part-2", 1, 0), first)
        val (second, afterSecond) = afterFirst.pop()
        assertEquals(TrailStop("part-1", 3, 40), second)
        val (none, empty) = afterSecond.pop()
        assertNull(none)
        assertEquals(ReaderTrail(), empty)
    }

    @Test
    fun theTrailAndEachPlaceSurviveSavingState() {
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val trail = ReaderTrail().push(TrailStop("a", 2, 120)).push(TrailStop("b"))

        val saved = with(ReaderTrail.Saver) { scope.save(trail) }!!

        assertEquals(trail, ReaderTrail.Saver.restore(saved))
    }

    @Test
    fun aDamagedSavedTrailRestoresTheReadablePartOnly() {
        assertEquals(ReaderTrail(listOf(TrailStop("a", 1, 2))),
            ReaderTrail.Saver.restore(listOf("a", 1, 2, "b", "x", 0, "c")))
    }
}
