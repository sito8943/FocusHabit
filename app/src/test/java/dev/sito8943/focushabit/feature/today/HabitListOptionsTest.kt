package dev.sito8943.focushabit.feature.today

import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.model.Streak
import dev.sito8943.focushabit.core.model.StreakUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitListOptionsTest {

    private fun row(id: Long, name: String, done: Boolean, frequency: FrequencyKind, category: Category?) = HabitRow(
        id = id, name = name, color = "#000000", icon = null, kind = HabitKind.Build, done = done, required = true,
        streak = Streak(0, 0, StreakUnit.Days), period = null, goal = null, goalProgress = 0,
        frequency = frequency, category = category,
    )

    private val mind = Category(10, "Mind", "#000000")
    private val health = Category(20, "Health", "#000000")

    private val rows = listOf(
        row(1, "read", done = true, FrequencyKind.PerWeek, mind),
        row(2, "Walk", done = false, FrequencyKind.Daily, null),
        row(3, "gym", done = false, FrequencyKind.Weekdays, health),
    )

    private fun ids(options: HabitListOptions) = options.apply(rows).map { it.id }

    @Test
    fun `default keeps the incoming order`() = assertEquals(listOf(1L, 2L, 3L), ids(HabitListOptions()))

    @Test
    fun `sorts by name ignoring case, status and frequency`() {
        assertEquals(listOf(3L, 1L, 2L), ids(HabitListOptions(sort = HabitSort.Name)))
        assertEquals(listOf(2L, 3L, 1L), ids(HabitListOptions(sort = HabitSort.Status)))
        assertEquals(listOf(2L, 3L, 1L), ids(HabitListOptions(sort = HabitSort.Frequency)))
    }

    @Test
    fun `filters combine and empty sets mean all`() {
        assertEquals(listOf(2L, 3L), ids(HabitListOptions(status = StatusFilter.Pending)))
        assertEquals(listOf(1L), ids(HabitListOptions(status = StatusFilter.Done)))
        assertEquals(listOf(2L), ids(HabitListOptions(categories = setOf(null))))
        assertEquals(listOf(3L), ids(HabitListOptions(status = StatusFilter.Pending, categories = setOf(20L, 10L))))
        assertEquals(listOf(1L, 3L), ids(HabitListOptions(frequencies = setOf(FrequencyKind.PerWeek, FrequencyKind.Weekdays))))
        assertEquals(2, HabitListOptions(status = StatusFilter.Done, frequencies = setOf(FrequencyKind.Daily)).activeFilters)
    }

    @Test
    fun `groups follow the category order with no category last`() {
        val groups = groupByCategory(rows, listOf(health, mind))
        assertEquals(listOf(health, mind, null), groups.map { it.first })
        assertEquals(listOf(listOf(3L), listOf(1L), listOf(2L)), groups.map { g -> g.second.map { it.id } })
        // Unknown category counts as none; empty groups disappear.
        assertEquals(listOf(null to listOf(1L, 2L, 3L)), groupByCategory(rows, emptyList()).map { it.first to it.second.map { r -> r.id } })
    }
}
