package com.caceras.aibrief

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditionStalenessTest {
    @Test
    fun nothingIsStaleBeforeTheFirstRefresh() {
        assertFalse(isEditionStale(lastAttempt = null, now = 10L * STALE_AFTER_MILLIS))
    }

    @Test
    fun aRecentRefreshIsNotRepeatedOnReturn() {
        assertFalse(isEditionStale(lastAttempt = 1_000L, now = 1_000L + STALE_AFTER_MILLIS - 1))
    }

    @Test
    fun returningAfterTheWindowRefreshes() {
        assertTrue(isEditionStale(lastAttempt = 1_000L, now = 1_000L + STALE_AFTER_MILLIS))
        assertTrue(isEditionStale(lastAttempt = 0L, now = 8L * 60L * 60L * 1000L))
    }
}
