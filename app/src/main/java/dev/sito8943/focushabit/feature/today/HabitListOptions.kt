package dev.sito8943.focushabit.feature.today

import dev.sito8943.focushabit.core.model.Category
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
enum class HabitSort { Default, Name, Status, Frequency }

enum class StatusFilter { All, Pending, Done }

/**
 * How the Today lists are shown. Empty [frequencies]/[categories] mean "all".
 * [categories] holds category ids; null stands for "no category".
 */
data class HabitListOptions(
    val sort: HabitSort = HabitSort.Default,
    val status: StatusFilter = StatusFilter.All,
    val frequencies: Set<FrequencyKind> = emptySet(),
    val categories: Set<Long?> = emptySet(),
    /** Today's list in one section per category (when there are categories). */
    val groupByCategory: Boolean = true,
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
                (categories.isEmpty() || row.category?.id in categories)
        }
        return when (sort) {
            HabitSort.Default -> filtered
            HabitSort.Name -> filtered.sortedBy { it.name.lowercase() }
            HabitSort.Status -> filtered.sortedBy { it.done }
            HabitSort.Frequency -> filtered.sortedBy { it.frequency }
        }
    }
}

/**
 * [rows] split by category, in [categories] order, "no category" last. Rows
 * whose category is unknown count as "no category". Empty groups are dropped.
 */
fun groupByCategory(rows: List<HabitRow>, categories: List<Category>): List<Pair<Category?, List<HabitRow>>> {
    val byId = rows.groupBy { row -> row.category?.id?.takeIf { id -> categories.any { it.id == id } } }
    return (categories.map { it.id } + listOf(null)).mapNotNull { id ->
        val group = byId[id] ?: return@mapNotNull null
        categories.firstOrNull { it.id == id } to group
    }
}
