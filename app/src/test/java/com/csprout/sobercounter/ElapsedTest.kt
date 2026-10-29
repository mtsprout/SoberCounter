package com.csprout.sobercounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ElapsedTest {
    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) = LocalDateTime.of(y, m, d, h, min)

    private fun assertFracs(e: Elapsed) {
        listOf(e.yearFrac, e.monthFrac, e.dayFrac, e.hourFrac).forEach {
            assertTrue("fraction out of range: $e", it in 0f..1f)
        }
    }

    @Test fun futureStartIsZero() {
        assertEquals(Elapsed(0, 0, 0, 0, 0f, 0f, 0f, 0f), elapsed(at(2030, 1, 1), at(2026, 1, 1)))
    }

    @Test fun simpleSpan() {
        val e = elapsed(at(2025, 8, 22, 12), at(2026, 10, 29, 15, 30))
        assertEquals(listOf(1L, 2L, 7L, 3L), listOf(e.years, e.months, e.days, e.hours))
        assertFracs(e)
    }

    @Test fun barsFillWithinTheirLargerCycle() {
        // 1y 2m 7d 21h 30m since the start: hours bar is 21.5/24 full, not near empty.
        val e = elapsed(at(2025, 8, 22, 12), at(2026, 10, 29, 9, 30))
        assertEquals(21L, e.hours)
        assertEquals(21.5f / 24f, e.hourFrac, 0.01f)
        assertTrue(e.dayFrac in 0.2f..0.3f)   // ~6.9 of 31 days
        assertTrue(e.monthFrac in 0.1f..0.2f) // ~2.2 of 12 months
        assertTrue(e.yearFrac in 0.2f..0.3f)  // ~1.2 of 5 years
    }

    @Test fun leapDayStartBeforeAnniversary() {
        val e = elapsed(at(2024, 2, 29), at(2025, 2, 27, 23, 59))
        assertEquals(0L, e.years)
        assertFracs(e)
    }

    @Test fun leapDayStartOnAnniversary() {
        // Feb 29 + 1 year clamps to Feb 28, which is also when the 1-year goal fires.
        val e = elapsed(at(2024, 2, 29), at(2025, 2, 28))
        assertEquals(1L, e.years)
        assertFracs(e)
    }

    @Test fun monthEndStartNeverExceedsOne() {
        // Jan 31 start: the bar used to overshoot 100% around Mar 28-30.
        var t = at(2026, 1, 31)
        while (t.isBefore(at(2027, 3, 5))) {
            assertFracs(elapsed(at(2026, 1, 31), t))
            t = t.plusHours(7)
        }
    }

    @Test fun everyStartDayOfYearStaysInRange() {
        var start = at(2023, 12, 25, 6, 15)
        repeat(60) {
            assertFracs(elapsed(start, start.plusDays(900).plusMinutes(13)))
            start = start.plusDays(6)
        }
    }

    @Test fun goalTargetsClampAtMonthEnd() {
        val start = at(2026, 1, 31)
        assertEquals(at(2026, 2, 28), Goal(1, 1, GoalUnit.MONTHS).targetFrom(start))
        assertEquals(at(2025, 2, 28), Goal(1, 1, GoalUnit.YEARS).targetFrom(at(2024, 2, 29)))
    }
}
