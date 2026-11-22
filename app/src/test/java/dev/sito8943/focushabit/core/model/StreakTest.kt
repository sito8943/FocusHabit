package dev.sito8943.focushabit.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {

    private val today = LocalDate.of(2026, 3, 1)

    @Test
    fun `counts consecutive days back from today across month ends`() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, streak(days, today))
    }

    @Test
    fun `is zero when today is not logged`() {
        assertEquals(0, streak(setOf(today.minusDays(1)), today))
    }
}
