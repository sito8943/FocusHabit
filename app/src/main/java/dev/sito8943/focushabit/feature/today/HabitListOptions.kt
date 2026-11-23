package dev.sito8943.focushabit.feature.today

import dev.sito8943.focushabit.core.model.Frequency

/** Frequency families, for sorting and filtering (quit habits are daily). */
enum class FrequencyKind {
    Daily, Weekdays, EveryNDays, PerWeek, PerMonth;

    companion object {
        fun of(frequency: Frequency): FrequencyKind = when (frequency) {
            Frequency.Daily -> Daily
            is Frequency.Weekdays -> Weekdays
            is Frequency.EveryNDays -> EveryNDays
            is Frequency.TimesPerWeek -> PerWeek
            is Frequency.TimesPerMonth -> PerMonth
        }
    }
}

/** Default: time of day first (earliest first), then creation order. */
enum class HabitSort { Default, Name, Status, Frequency, Category }

enum class StatusFilter { All, Pending, Done }

/**
 * How the Today lists are shown. Empty [frequencies]/[categories] mean "all".
 * In [categories], null stands for "no category".
 */
data class HabitListOptions(
    val sort: HabitSort = HabitSort.Default,
    val status: StatusFilter = StatusFilter.All,
    val frequencies: Set<FrequencyKind> = emptySet(),
    val categories: Set<String?> = emptySet(),
) {
    /** Number of active filters (sort not included), for the badge. */
    val activeFilters: Int
        get() = (if (status != StatusFilter.All) 1 else 0) +
            (if (frequencies.isNotEmpty()) 1 else 0) +
            (if (categories.isNotEmpty()) 1 else 0)

    val isDefault: Boolean get() = this == HabitListOptions()

    /** Filters then sorts [rows], which arrive in the default order (ties keep it). */
    fun apply(rows: List<HabitRow>): List<HabitRow> {
        val filtered = rows.filter { row ->
            when (status) {
                StatusFilter.All -> true
                StatusFilter.Pending -> !row.done
                StatusFilter.Done -> row.done
            } &&
                (frequencies.isEmpty() || row.frequency in frequencies) &&
                (categories.isEmpty() || row.category in categories)
        }
        return when (sort) {
            HabitSort.Default -> filtered
            HabitSort.Name -> filtered.sortedBy { it.name.lowercase() }
            HabitSort.Status -> filtered.sortedBy { it.done }
            HabitSort.Frequency -> filtered.sortedBy { it.frequency }
            HabitSort.Category -> filtered.sortedWith(compareBy(nullsLast()) { it.category?.lowercase() })
        }
    }
}
