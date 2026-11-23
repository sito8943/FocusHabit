package dev.sito8943.focushabit.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class StreakUnit { Days, Times, Weeks, Months }

data class Streak(val current: Int, val best: Int, val unit: StreakUnit)

/** Progress inside the current week/month of a flexible habit. */
data class PeriodProgress(val done: Int, val target: Int, val unit: StreakUnit)

/**
 * Everything that depends on a habit's kind and frequency, computed from its
 * logged days. For a quit habit [logs] are relapses.
 */
class HabitSchedule(
    val habit: Habit,
    val logs: Set<LocalDate>,
    /** First day of the week for N-per-week habits (Settings). */
    private val weekStart: DayOfWeek = DayOfWeek.MONDAY,
) {

    /** First day that counts: creation, or an earlier day logged afterwards. */
    val start: LocalDate =
        listOfNotNull(habit.startDate.takeIf { it != LocalDate.MIN }, logs.minOrNull()).minOrNull() ?: LocalDate.now()

    private val frequency = if (habit.kind == HabitKind.Quit) Frequency.Daily else habit.frequency

    val isFlexible: Boolean get() = frequency is Frequency.TimesPerWeek || frequency is Frequency.TimesPerMonth

    /** Build: logged that day. Quit: no relapse that day. */
    fun isDone(day: LocalDate): Boolean = when (habit.kind) {
        HabitKind.Build -> day in logs
        HabitKind.Quit -> day >= start && day !in logs
    }

    /**
     * Expected on [day]. Flexible habits (N per week/month) are never required
     * on a given day: they count only on the days they are done.
     */
    fun isDue(day: LocalDate): Boolean {
        if (day < start) return false
        if (habit.kind == HabitKind.Build && day in logs) return true
        return when (val f = frequency) {
            Frequency.Daily -> true
            is Frequency.Weekdays -> day.dayOfWeek in f.days
            is Frequency.EveryNDays -> {
                val last = logs.filter { it < day }.maxOrNull() ?: return true
                ChronoUnit.DAYS.between(last, day) >= f.days
            }
            is Frequency.TimesPerWeek, is Frequency.TimesPerMonth -> false
        }
    }

    /** Worth listing as "for today": due, or a flexible habit whose quota is still open. */
    fun isOpen(day: LocalDate): Boolean = isDue(day) || (isFlexible && periodProgress(day).let { it!!.done < it.target })

    /**
     * Next day from [today] on (inclusive) when a build habit is still to be
     * done: due and not logged, or inside a period whose quota is open. Null
     * for quit habits (every day counts) or if nothing is found within a year.
     */
    fun nextDue(today: LocalDate): LocalDate? {
        if (habit.kind == HabitKind.Quit) return null
        return generateSequence(maxOf(today, start)) { it.plusDays(1) }
            .take(NEXT_DUE_LOOKAHEAD)
            .firstOrNull { it !in logs && isOpen(it) }
    }

    fun periodProgress(day: LocalDate): PeriodProgress? = when (val f = frequency) {
        is Frequency.TimesPerWeek -> PeriodProgress(logs.count { weekOf(it) == weekOf(day) }, f.times, StreakUnit.Weeks)
        is Frequency.TimesPerMonth ->
            PeriodProgress(logs.count { YearMonth.from(it) == YearMonth.from(day) }, f.times, StreakUnit.Months)
        else -> null
    }

    fun streak(today: LocalDate): Streak = when (habit.kind) {
        HabitKind.Quit -> Streak(cleanDays(today), bestClean(today), StreakUnit.Days)
        HabitKind.Build -> when (val f = frequency) {
            Frequency.Daily -> Streak(streak(logs, today), bestStreak(logs), StreakUnit.Days)
            is Frequency.Weekdays -> scheduledDaysStreak(f.days, today)
            is Frequency.EveryNDays -> intervalStreak(f.days, today)
            is Frequency.TimesPerWeek -> periodStreak(today, f.times, StreakUnit.Weeks) { weekOf(it) }
            is Frequency.TimesPerMonth -> periodStreak(today, f.times, StreakUnit.Months) { it.withDayOfMonth(1) }
        }
    }

    /** Build: times done in total. Quit: current days in a row without a relapse. */
    fun goalProgress(today: LocalDate): Int = when (habit.kind) {
        HabitKind.Build -> logs.count { it <= today }
        HabitKind.Quit -> cleanDays(today)
    }

    fun goalReached(today: LocalDate): Boolean = habit.goal?.let { goalProgress(today) >= it } ?: false

    /** Days without a relapse, today included if it is clean so far. */
    private fun cleanDays(today: LocalDate): Int {
        val lastRelapse = logs.filter { it <= today }.maxOrNull()
        val from = lastRelapse ?: start.minusDays(1)
        return ChronoUnit.DAYS.between(from, today).toInt().coerceAtLeast(0)
    }

    private fun bestClean(today: LocalDate): Int {
        val relapses = logs.filter { it <= today }.sorted()
        var best = 0
        var previous = start.minusDays(1)
        for (relapse in relapses) {
            best = maxOf(best, ChronoUnit.DAYS.between(previous, relapse).toInt() - 1)
            previous = relapse
        }
        return maxOf(best, ChronoUnit.DAYS.between(previous, today).toInt())
    }

    private fun scheduledDaysStreak(days: Set<DayOfWeek>, today: LocalDate): Streak {
        var current = 0
        var counting = true
        var best = 0
        var run = 0
        // Walk forward for the best run, backward for the current one.
        var day = start
        while (day <= today) {
            if (day.dayOfWeek in days) {
                if (day in logs) run++ else if (day != today) run = 0
                best = maxOf(best, run)
            }
            day = day.plusDays(1)
        }
        day = today
        while (counting && day >= start) {
            if (day.dayOfWeek in days) {
                when {
                    day in logs -> current++
                    day == today -> Unit // still has the rest of the day
                    else -> counting = false
                }
            }
            day = day.minusDays(1)
        }
        return Streak(current, maxOf(best, current), StreakUnit.Days)
    }

    /** Completions in a row with gaps of at most [every] days; alive while the next one is not overdue. */
    private fun intervalStreak(every: Int, today: LocalDate): Streak {
        val done = logs.filter { it <= today }.sorted()
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in done) {
            run = if (previous != null && ChronoUnit.DAYS.between(previous, day) <= every) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        val alive = previous != null && ChronoUnit.DAYS.between(previous, today) <= every
        return Streak(if (alive) run else 0, best, StreakUnit.Times)
    }

    private fun periodStreak(
        today: LocalDate,
        target: Int,
        unit: StreakUnit,
        periodOf: (LocalDate) -> LocalDate,
    ): Streak {
        val counts = logs.filter { it <= today }.groupingBy(periodOf).eachCount()
        val step: (LocalDate) -> LocalDate = if (unit == StreakUnit.Weeks) {
            { it.minusWeeks(1) }
        } else {
            { it.minusMonths(1) }
        }
        val first = periodOf(start)
        var period = periodOf(today)
        // The current period counts once complete; until then the streak lives on the previous ones.
        if ((counts[period] ?: 0) < target) period = step(period)
        var current = 0
        while (period >= first && (counts[period] ?: 0) >= target) {
            current++
            period = step(period)
        }
        var best = 0
        var run = 0
        period = periodOf(today)
        while (period >= first) {
            if ((counts[period] ?: 0) >= target) {
                run++
                best = maxOf(best, run)
            } else if (period != periodOf(today)) {
                run = 0
            }
            period = step(period)
        }
        return Streak(current, maxOf(best, current), unit)
    }

    private fun weekOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(weekStart))
}

private const val NEXT_DUE_LOOKAHEAD = 400

/**
 * Share of the habits due on [day] that are done (0..1), or null if nothing
 * was due. Drives the day strip, the dots and the "perfect days" streak.
 */
fun dayCompletion(schedules: List<HabitSchedule>, day: LocalDate): Float? {
    val due = schedules.filter { it.isDue(day) }
    if (due.isEmpty()) return null
    return due.count { it.isDone(day) }.toFloat() / due.size
}

/** Consecutive days where everything due was done; days with nothing due are skipped. */
fun perfectDaysStreak(completion: (LocalDate) -> Float?, today: LocalDate, limit: Int = 366): Int {
    var count = 0
    var day = today
    repeat(limit) {
        when (completion(day)) {
            null -> Unit
            1f -> count++
            else -> if (day != today) return count // today still has time
        }
        day = day.minusDays(1)
    }
    return count
}
