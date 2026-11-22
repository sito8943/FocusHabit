package dev.sito8943.focushabit.core.model

import java.time.LocalDate

data class Habit(
    val id: Long,
    val name: String,
    /** `#rrggbb`, one of [HabitColors.palette] when created here. */
    val color: String,
)

data class HabitLog(
    val habitId: Long,
    val date: LocalDate,
)

/** The six swatches of the web form, in the same order. The first is the default. */
object HabitColors {
    val palette = listOf("#e74c3c", "#3498db", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c")
    val default = palette.first()
}

/**
 * Consecutive days with a log, walking back from [today]. If [today] is not
 * logged the streak is 0: same rule as the web (`getStreak`).
 */
fun streak(loggedDates: Set<LocalDate>, today: LocalDate): Int {
    var count = 0
    var day = today
    while (day in loggedDates) {
        count++
        day = day.minusDays(1)
    }
    return count
}
