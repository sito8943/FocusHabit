package dev.sito8943.focushabit.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/** Build = something to do. Quit = something to stop doing; its logs are relapses. */
enum class HabitKind(val storage: String) {
    Build("build"),
    Quit("quit");

    companion object {
        fun from(value: String): HabitKind = entries.firstOrNull { it.storage == value } ?: Build
    }
}

sealed interface Frequency {
    data object Daily : Frequency

    /** Only on these days of the week. */
    data class Weekdays(val days: Set<DayOfWeek>) : Frequency

    /** Due again [days] days after the last time it was done. */
    data class EveryNDays(val days: Int) : Frequency

    /** Any [times] days of each week (first day from Settings). */
    data class TimesPerWeek(val times: Int) : Frequency

    /** Any [times] days of each calendar month. */
    data class TimesPerMonth(val times: Int) : Frequency

    /** Stored as two columns: a type and a number (a bitmask, Monday = bit 0, for weekdays). */
    val storage: Pair<String, Int>
        get() = when (this) {
            Daily -> "daily" to 1
            is Weekdays -> "weekdays" to days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }
            is EveryNDays -> "interval" to days
            is TimesPerWeek -> "week" to times
            is TimesPerMonth -> "month" to times
        }

    companion object {
        fun from(type: String, value: Int): Frequency = when (type) {
            "weekdays" -> Weekdays(DayOfWeek.entries.filter { value and (1 shl (it.value - 1)) != 0 }.toSet())
                .takeIf { it.days.isNotEmpty() } ?: Daily
            "interval" -> EveryNDays(value.coerceAtLeast(1))
            "week" -> TimesPerWeek(value.coerceIn(1, 7))
            "month" -> TimesPerMonth(value.coerceIn(1, 31))
            else -> Daily
        }
    }
}

data class Habit(
    val id: Long,
    val name: String,
    /** `#rrggbb`, one of [HabitColors.palette] when created here. */
    val color: String,
    /** Font Awesome 7 Free solid icon name, or null to show the initial. */
    val icon: String? = null,
    val kind: HabitKind = HabitKind.Build,
    /** Quit habits are always daily. */
    val frequency: Frequency = Frequency.Daily,
    /** Build: done this many times in total. Quit: this many days in a row without it. */
    val goal: Int? = null,
    val startDate: LocalDate = LocalDate.MIN,
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
