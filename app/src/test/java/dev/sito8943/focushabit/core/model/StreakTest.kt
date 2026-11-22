package dev.sito8943.focushabit.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {

    private val today = LocalDate.of(2026, 3, 1) // Sunday

    @Test
    fun `counts consecutive days back from today across month ends`() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, streak(days, today))
    }

    @Test
    fun `stays alive from yesterday while today is not logged yet`() {
        assertEquals(2, streak(setOf(today.minusDays(1), today.minusDays(2)), today))
    }

    @Test
    fun `is zero when neither today nor yesterday is logged`() {
        assertEquals(0, streak(setOf(today.minusDays(2)), today))
    }

    @Test
    fun `best streak is the longest run`() {
        val days = setOf(1, 2, 3, 5, 6, 7, 8, 10).map { LocalDate.of(2026, 1, it) }.toSet()
        assertEquals(4, bestStreak(days))
        assertEquals(0, bestStreak(emptySet()))
    }

    @Test
    fun `weekday counts only the window`() {
        val days = setOf(today, today.minusWeeks(1), today.minusWeeks(12), today.minusDays(1))
        val counts = weekdayCounts(days, today, weeks = 12)
        assertEquals(2, counts[DayOfWeek.SUNDAY])
        assertEquals(1, counts[DayOfWeek.SATURDAY])
        assertEquals(0, counts[DayOfWeek.MONDAY])
    }
}
