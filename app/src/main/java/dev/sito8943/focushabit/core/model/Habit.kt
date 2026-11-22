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
