package com.csprout.sobercounter

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Calendar-accurate elapsed time. Each *Frac is how far through the current
 * unit we are (0..1), used to fill that unit's bar.
 */
data class Elapsed(
    val years: Long, val months: Long, val days: Long, val hours: Long,
    val yearFrac: Float, val monthFrac: Float, val dayFrac: Float, val hourFrac: Float,
)

fun elapsed(start: LocalDateTime, now: LocalDateTime): Elapsed {
    if (!now.isAfter(start)) return Elapsed(0, 0, 0, 0, 0f, 0f, 0f, 0f)
    val years = ChronoUnit.YEARS.between(start, now)
    var cur = start.plusYears(years)
    val months = ChronoUnit.MONTHS.between(cur, now)
    val monthStart = cur.plusMonths(months)
    cur = monthStart
    val days = ChronoUnit.DAYS.between(cur, now)
    cur = cur.plusDays(days)
    val hours = ChronoUnit.HOURS.between(cur, now)
    cur = cur.plusHours(hours)
    val secIntoHour = ChronoUnit.SECONDS.between(cur, now).toFloat()

    val hourFrac = secIntoHour / 3600f
    val dayFrac = (hours + hourFrac) / 24f
    val monthLen = ChronoUnit.DAYS.between(monthStart, monthStart.plusMonths(1)).toFloat()
    val monthFrac = (days + dayFrac) / monthLen
    val yearFrac = (months + monthFrac) / 12f
    return Elapsed(years, months, days, hours, yearFrac, monthFrac, dayFrac, hourFrac)
}
