package dev.sito8943.focushabit.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {

    private val today = LocalDate.of(2026, 11, 22) // Sunday
    private fun d(daysAgo: Long) = today.minusDays(daysAgo)
    private fun habit(
        frequency: Frequency = Frequency.Daily,
        kind: HabitKind = HabitKind.Build,
        start: LocalDate = d(30),
        goal: Int? = null,
    ) = Habit(1, "h", "#000000", kind = kind, frequency = frequency, goal = goal, startDate = start)

    @Test
    fun `frequency round-trips through storage`() {
        val all = listOf(
            Frequency.Daily,
            Frequency.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)),
            Frequency.EveryNDays(3),
            Frequency.TimesPerWeek(2),
            Frequency.TimesPerMonth(5),
        )
        all.forEach { f -> val (t, v) = f.storage; assertEquals(f, Frequency.from(t, v)) }
    }

    @Test
    fun `weekdays are due only on their days and streak skips the others`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        // Fri 20, Wed 18, Mon 16 done; Fri 13 missed.
        val s = HabitSchedule(habit(Frequency.Weekdays(days)), setOf(d(2), d(4), d(6)))
        assertFalse(s.isDue(today))
        assertTrue(s.isDue(d(2)))
        assertEquals(3, s.streak(today).current)
    }

    @Test
    fun `every n days is due again n days after the last time`() {
        val s = HabitSchedule(habit(Frequency.EveryNDays(3)), setOf(d(2), d(5), d(8)))
        assertFalse(s.isDue(today)) // 2 days since last
        assertTrue(s.isDue(today.plusDays(1)))
        assertEquals(Streak(3, 3, StreakUnit.Times), s.streak(today))
        assertEquals(0, HabitSchedule(habit(Frequency.EveryNDays(3)), setOf(d(4))).streak(today).current)
    }

    @Test
    fun `once a week done on Sunday still counts on Monday when weeks start on Sunday`() {
        val monday = today.plusDays(1)
        val sundayWeek = HabitSchedule(habit(Frequency.TimesPerWeek(1)), setOf(today), DayOfWeek.SUNDAY)
        assertEquals(PeriodProgress(1, 1, StreakUnit.Weeks), sundayWeek.periodProgress(monday))
        assertFalse(sundayWeek.isOpen(monday))
        assertEquals(1, sundayWeek.streak(monday).current)

        val mondayWeek = HabitSchedule(habit(Frequency.TimesPerWeek(1)), setOf(today), DayOfWeek.MONDAY)
        assertEquals(PeriodProgress(0, 1, StreakUnit.Weeks), mondayWeek.periodProgress(monday))
    }

    @Test
    fun `next due day per frequency`() {
        // today = Sunday Nov 22.
        assertEquals(today, HabitSchedule(habit(), emptySet()).nextDue(today))
        assertEquals(today.plusDays(1), HabitSchedule(habit(), setOf(today)).nextDue(today))
        val mwf = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(today.plusDays(1), HabitSchedule(habit(Frequency.Weekdays(mwf)), emptySet()).nextDue(today))
        assertEquals(d(-2), HabitSchedule(habit(Frequency.EveryNDays(3)), setOf(d(1))).nextDue(today))
        // Weekly quota met this week (Mon-Sun): next week starts tomorrow.
        assertEquals(today.plusDays(1), HabitSchedule(habit(Frequency.TimesPerWeek(1)), setOf(d(3))).nextDue(today))
        // Same log, weeks from Sunday: Nov 19 is last week, so today is open.
        assertEquals(today, HabitSchedule(habit(Frequency.TimesPerWeek(1)), setOf(d(3)), DayOfWeek.SUNDAY).nextDue(today))
        assertEquals(null, HabitSchedule(habit(kind = HabitKind.Quit), emptySet()).nextDue(today))
    }

    @Test
    fun `times per week counts complete weeks and the current one once full`() {
        // Weeks (Mon-Sun): this week Nov 16-22, previous Nov 9-15, Nov 2-8.
        val logs = setOf(d(1), d(3), d(8), d(10), d(15))
        val s = HabitSchedule(habit(Frequency.TimesPerWeek(2)), logs)
        assertEquals(PeriodProgress(2, 2, StreakUnit.Weeks), s.periodProgress(today))
        assertEquals(2, s.streak(today).current) // this week + previous; Nov 2-8 has 1
        assertFalse(s.isDue(today))
        assertFalse(s.isOpen(today)) // quota met
    }

    @Test
    fun `quit counts clean days since the last relapse`() {
        val s = HabitSchedule(habit(kind = HabitKind.Quit, start = d(20), goal = 10), setOf(d(7)))
        assertEquals(7, s.streak(today).current)
        assertEquals(13, s.streak(today).best) // d(20)..d(8)
        assertTrue(s.isDone(today))
        assertFalse(s.goalReached(today))
        assertEquals(0, HabitSchedule(habit(kind = HabitKind.Quit), setOf(today)).streak(today).current)
    }

    @Test
    fun `perfect days skip days with nothing due and spare today`() {
        val completion = mapOf(d(0) to 0.5f, d(1) to 1f, d(3) to 1f, d(4) to 0.5f)
        assertEquals(2, perfectDaysStreak({ completion[it] }, today))
        assertNull(dayCompletion(emptyList(), today))
    }
}
