package dev.sito8943.focushabit.core.data

import androidx.room.withTransaction
import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.core.model.Frequency
import dev.sito8943.focushabit.core.model.Habit
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.model.HabitLog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
    private val categories = database.categories()

    fun observeHabits(code: String): Flow<List<Habit>> =
        habits.observeActive(code).map { rows -> rows.map { it.toModel() } }

    fun observeCategories(code: String): Flow<List<Category>> =
        categories.observe(code).map { rows -> rows.map { Category(it.id, it.name, it.color, it.icon) } }

    /** New category when [Category.id] is 0, otherwise an edit. Returns its id. */
    suspend fun saveCategory(code: String, category: Category): Long {
        if (category.id == 0L) {
            return categories.insert(
                CategoryEntity(
                    code = code,
                    name = category.name,
                    color = category.color,
                    icon = category.icon,
                    createdAt = now().toString(),
                ),
            )
        }
        val current = categories.get(category.id) ?: return category.id
        categories.update(current.copy(name = category.name, color = category.color, icon = category.icon))
        return category.id
    }

    /** Its habits stay, without a category. */
    suspend fun deleteCategory(id: Long) = database.withTransaction {
        habits.clearCategory(id)
        categories.delete(id)
    }

    suspend fun getHabit(id: Long): Habit? = habits.get(id)?.takeIf { it.deletedAt == null }?.toModel()

    fun observeLogs(code: String): Flow<List<HabitLog>> =
        logs.observeActive(code).map { rows ->
            rows.mapNotNull { row ->
                runCatching { LocalDate.parse(row.logDate) }.getOrNull()?.let { HabitLog(row.habitId, it) }
            }
        }

    /** New habit when [id] is null, otherwise an edit of that one. */
    suspend fun saveHabit(code: String, id: Long?, draft: Habit) {
        val (frequency, frequencyValue) =
            (if (draft.kind == HabitKind.Quit) Frequency.Daily else draft.frequency).storage
        if (id == null) {
            habits.insert(
                HabitEntity(
                    code = code,
                    name = draft.name,
                    color = draft.color,
                    createdAt = now().toString(),
                    icon = draft.icon,
                    kind = draft.kind.storage,
                    frequency = frequency,
                    frequencyValue = frequencyValue,
                    goal = draft.goal,
                    startDate = LocalDate.now().toString(),
                    time = draft.time?.toStorage(),
                    categoryId = draft.categoryId,
                ),
            )
        } else {
            val current = habits.get(id) ?: return
            habits.update(
                current.copy(
                    name = draft.name,
                    color = draft.color,
                    icon = draft.icon,
                    kind = draft.kind.storage,
                    frequency = frequency,
                    frequencyValue = frequencyValue,
                    goal = draft.goal,
                    time = draft.time?.toStorage(),
                    categoryId = draft.categoryId,
                ),
            )
        }
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

private fun HabitEntity.toModel() = Habit(
    id = id,
    name = name,
    color = color,
    icon = icon,
    kind = HabitKind.from(kind),
    frequency = Frequency.from(frequency, frequencyValue),
    goal = goal,
    startDate = runCatching { LocalDate.parse(startDate) }
        .getOrElse { runCatching { LocalDate.parse(createdAt.take(10)) }.getOrDefault(LocalDate.MIN) },
    time = time?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    categoryId = categoryId,
)

private fun LocalTime.toStorage(): String = "%02d:%02d".format(hour, minute)
