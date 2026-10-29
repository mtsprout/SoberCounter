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

    // ChronoUnit.between and plusYears/plusMonths disagree around Feb 29 and month ends
    // (Feb 29 + 1 year clamps to Feb 28), so settle each count with the same plus operation we
    // later use for boundaries. That keeps every fraction within 0..1.
    var years = ChronoUnit.YEARS.between(start, now)
    while (start.plusYears(years).isAfter(now)) years--
    while (!start.plusYears(years + 1).isAfter(now)) years++
    val yearBase = start.plusYears(years)

    var months = ChronoUnit.MONTHS.between(yearBase, now)
    while (yearBase.plusMonths(months).isAfter(now)) months--
    while (!yearBase.plusMonths(months + 1).isAfter(now)) months++
    val monthStart = yearBase.plusMonths(months)
    val monthEnd = yearBase.plusMonths(months + 1)

    val days = ChronoUnit.DAYS.between(monthStart, now)
    var cur = monthStart.plusDays(days)
    val hours = ChronoUnit.HOURS.between(cur, now)
    cur = cur.plusHours(hours)
    val secIntoHour = ChronoUnit.SECONDS.between(cur, now).toFloat()

    val hourFrac = secIntoHour / 3600f
    val dayFrac = (hours + hourFrac) / 24f
    val monthLen = ChronoUnit.DAYS.between(monthStart, monthEnd).toFloat()
    val monthFrac = ((days + dayFrac) / monthLen).coerceIn(0f, 1f)
    val yearFrac = ((months + monthFrac) / 12f).coerceIn(0f, 1f)
    return Elapsed(years, months, days, hours, yearFrac, monthFrac, dayFrac, hourFrac)
}
