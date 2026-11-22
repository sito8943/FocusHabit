package dev.sito8943.focushabit.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.di.DateState
import dev.sito8943.focushabit.core.model.streak
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitRow(
    val id: Long,
    val name: String,
    val color: String,
    /** Done on the selected day. */
    val done: Boolean,
    /** Counted from today, not from the selected day (same as the web). */
    val streak: Int,
)

data class TodayUiState(
    val date: LocalDate,
    val today: LocalDate,
    val habits: List<HabitRow>,
    /** Share of the current habits done on each day that has any log (0..1]. */
    val completion: Map<LocalDate, Float>,
    /** Consecutive days with at least one habit done. */
    val overallStreak: Int,
    val loading: Boolean,
) {
    val isToday: Boolean get() = date == today
    val doneCount: Int get() = habits.count { it.done }
    val progress: Float get() = if (habits.isEmpty()) 0f else doneCount.toFloat() / habits.size
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: HabitRepository,
    private val codeStore: SyncCodeStore,
    private val dates: DateState,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = codeStore.code
        .flatMapLatest { code ->
            combine(
                repository.observeHabits(code),
                repository.observeLogs(code),
                dates.selected,
                dates.today,
            ) { habits, logs, selected, today ->
                val datesByHabit = logs.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }
                val total = habits.size.coerceAtLeast(1)
                val completion = logs
                    .distinctBy { it.habitId to it.date }
                    .groupingBy { it.date }
                    .eachCount()
                    .mapValues { (_, count) -> (count.toFloat() / total).coerceAtMost(1f) }
                TodayUiState(
                    date = selected,
                    today = today,
                    habits = habits.map { habit ->
                        val days = datesByHabit[habit.id].orEmpty()
                        HabitRow(
                            id = habit.id,
                            name = habit.name,
                            color = habit.color,
                            done = selected in days,
                            streak = streak(days, today),
                        )
                    },
                    completion = completion,
                    overallStreak = streak(completion.keys, today),
                    loading = false,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayUiState(
                date = dates.selected.value,
                today = dates.today.value,
                habits = emptyList(),
                completion = emptyMap(),
                overallStreak = 0,
                loading = true,
            ),
        )

    /** Empty after trimming is ignored, like the web form. */
    fun addHabit(name: String, color: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addHabit(codeStore.code.value, trimmed, color) }
    }

    fun toggle(habitId: Long) {
        val date = state.value.date
        viewModelScope.launch { repository.toggleLog(habitId, date) }
    }

    fun select(day: LocalDate) {
        dates.selected.value = day
    }

    fun backToToday() {
        dates.selected.value = dates.today.value
    }
}
