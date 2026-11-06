package com.caceras.aibrief.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CardDateTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun thisYearLeavesTheYearOut() {
        assertEquals("3 Oct", cardDate(Instant.parse("2026-10-03T04:27:25Z"), utc, LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun anEarlierYearIsNamed() {
        assertEquals("3 Oct 2026", cardDate(Instant.parse("2026-10-03T04:27:25Z"), utc, LocalDate.of(2027, 1, 2)))
    }

    @Test
    fun theYearFollowsTheReadersTimeZone() {
        // 23:30 UTC on New Year's Eve is already New Year's Day in Stockholm.
        val instant = Instant.parse("2026-12-31T23:30:00Z")
        val today = LocalDate.of(2027, 1, 1)
        assertEquals("1 Jan", cardDate(instant, ZoneId.of("Europe/Stockholm"), today))
        assertEquals("31 Dec 2026", cardDate(instant, utc, today))
    }
}
