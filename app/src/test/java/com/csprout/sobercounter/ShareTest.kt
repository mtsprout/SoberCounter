package com.csprout.sobercounter

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareTest {
    private fun at(start: String, now: String) = elapsed(LocalDateTime.parse(start), LocalDateTime.parse(now))

    @Test fun skipsZeroUnitsAndPluralizes() {
        assertEquals("1 year, 3 months, 21 days", shareSentence(at("2024-01-01T08:00", "2025-04-22T12:00")))
        assertEquals("1 year, 21 days", shareSentence(at("2024-01-01T08:00", "2025-01-22T12:00")))
        assertEquals("2 years", shareSentence(at("2023-01-01T08:00", "2025-01-01T12:00")))
        assertEquals("1 month, 1 day", shareSentence(at("2025-01-01T08:00", "2025-02-02T12:00")))
    }

    @Test fun hoursOnlyBeforeFirstFullDay() {
        assertEquals("7 hours", shareSentence(at("2025-01-01T08:00", "2025-01-01T15:30")))
        assertEquals("1 hour", shareSentence(at("2025-01-01T08:00", "2025-01-01T09:30")))
        assertEquals("Just getting started", shareSentence(at("2025-01-01T08:00", "2025-01-01T08:20")))
        assertEquals("3 days", shareSentence(at("2025-01-01T08:00", "2025-01-04T20:00")))
    }
}
