package dev.sito8943.focushabit.core.data

import androidx.room.withTransaction
import dev.sito8943.focushabit.core.model.Habit
import dev.sito8943.focushabit.core.model.HabitLog
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The only door to Room for the UI. Every write is local and instant; the
 * sync coordinator notices the pending rows and pushes them on its own.
 */
class HabitRepository(
    private val database: FocusHabitDatabase,
    private val now: () -> Instant = Instant::now,
) {

    private val habits = database.habits()
    private val logs = database.logs()

    fun observeHabits(code: String): Flow<List<Habit>> =
        habits.observeActive(code).map { rows -> rows.map { Habit(it.id, it.name, it.color) } }

    fun observeLogs(code: String): Flow<List<HabitLog>> =
        logs.observeActive(code).map { rows ->
            rows.mapNotNull { row ->
                runCatching { LocalDate.parse(row.logDate) }.getOrNull()?.let { HabitLog(row.habitId, it) }
            }
        }

    suspend fun addHabit(code: String, name: String, color: String) {
        habits.insert(
            HabitEntity(code = code, name = name, color = color, createdAt = now().toString()),
        )
    }

    /**
     * Never pushed: gone for real. Already on the server: hidden now, soft
     * deleted there (habit and its logs) by the next sync.
     */
    suspend fun deleteHabit(habitId: Long) = database.withTransaction {
        val habit = habits.get(habitId) ?: return@withTransaction
        if (habit.remoteId == null) {
            habits.delete(habit.id)
        } else {
            logs.deleteUnsyncedOf(habit.id)
            habits.markDeleted(habit.id, now().toString())
        }
    }

    /** Marks [date] as done, or undone if it already was (every duplicate included). */
    suspend fun toggleLog(habitId: Long, date: LocalDate) = database.withTransaction {
        val day = date.toString()
        val active = logs.activeOn(habitId, day)
        if (active.isEmpty()) {
            logs.insert(HabitLogEntity(habitId = habitId, logDate = day))
        } else {
            val at = now().toString()
            active.forEach { log ->
                if (log.remoteId == null) logs.delete(log.id) else logs.markDeleted(log.id, at)
            }
        }
    }
}
