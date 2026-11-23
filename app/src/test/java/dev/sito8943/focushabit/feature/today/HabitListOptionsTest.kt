package dev.sito8943.focushabit.feature.today

import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.model.Streak
import dev.sito8943.focushabit.core.model.StreakUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitListOptionsTest {

    private fun row(id: Long, name: String, done: Boolean, frequency: FrequencyKind, category: String?) = HabitRow(
        id = id, name = name, color = "#000000", icon = null, kind = HabitKind.Build, done = done, required = true,
        streak = Streak(0, 0, StreakUnit.Days), period = null, goal = null, goalProgress = 0,
        frequency = frequency, category = category,
    )

    private val rows = listOf(
        row(1, "read", done = true, FrequencyKind.PerWeek, "Mind"),
        row(2, "Walk", done = false, FrequencyKind.Daily, null),
        row(3, "gym", done = false, FrequencyKind.Weekdays, "Health"),
    )

    private fun ids(options: HabitListOptions) = options.apply(rows).map { it.id }

    @Test
    fun `default keeps the incoming order`() = assertEquals(listOf(1L, 2L, 3L), ids(HabitListOptions()))

    @Test
    fun `sorts by name ignoring case, status, frequency and category with none last`() {
        assertEquals(listOf(3L, 1L, 2L), ids(HabitListOptions(sort = HabitSort.Name)))
        assertEquals(listOf(2L, 3L, 1L), ids(HabitListOptions(sort = HabitSort.Status)))
        assertEquals(listOf(2L, 3L, 1L), ids(HabitListOptions(sort = HabitSort.Frequency)))
        assertEquals(listOf(3L, 1L, 2L), ids(HabitListOptions(sort = HabitSort.Category)))
    }

    @Test
    fun `filters combine and empty sets mean all`() {
        assertEquals(listOf(2L, 3L), ids(HabitListOptions(status = StatusFilter.Pending)))
        assertEquals(listOf(1L), ids(HabitListOptions(status = StatusFilter.Done)))
        assertEquals(listOf(2L), ids(HabitListOptions(categories = setOf(null))))
        assertEquals(listOf(3L), ids(HabitListOptions(status = StatusFilter.Pending, categories = setOf("Health", "Mind"))))
        assertEquals(listOf(1L, 3L), ids(HabitListOptions(frequencies = setOf(FrequencyKind.PerWeek, FrequencyKind.Weekdays))))
        assertEquals(2, HabitListOptions(status = StatusFilter.Done, frequencies = setOf(FrequencyKind.Daily)).activeFilters)
    }
}
