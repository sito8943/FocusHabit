package dev.sito8943.focushabit.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Consecutive days with a log, walking back from [today].
 *
 * A streak stays alive until the day is over: if [today] is not logged yet,
 * it counts from yesterday. The web shows 0 there, which resets everyone's
 * streak every morning.
 */
fun streak(loggedDates: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in loggedDates) today else today.minusDays(1)
    var count = 0
    while (day in loggedDates) {
        count++
        day = day.minusDays(1)
    }
    return count
}

/** Longest run of consecutive logged days ever. */
fun bestStreak(loggedDates: Set<LocalDate>): Int {
    var best = 0
    for (day in loggedDates) {
        if (day.minusDays(1) in loggedDates) continue // not the start of a run
        var length = 0
        var cursor = day
        while (cursor in loggedDates) {
            length++
            cursor = cursor.plusDays(1)
        }
        if (length > best) best = length
    }
    return best
}

/** Logs per weekday in the last [weeks] weeks up to [today], Monday first. */
fun weekdayCounts(loggedDates: Set<LocalDate>, today: LocalDate, weeks: Int = 12): Map<DayOfWeek, Int> {
    val from = today.minusWeeks(weeks.toLong()).plusDays(1)
    val counts = DayOfWeek.entries.associateWith { 0 }.toMutableMap()
    loggedDates.filter { it in from..today }.forEach { counts[it.dayOfWeek] = counts.getValue(it.dayOfWeek) + 1 }
    return counts
}
