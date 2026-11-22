package dev.sito8943.focushabit.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.di.DateState
import dev.sito8943.focushabit.core.model.Habit
import dev.sito8943.focushabit.core.model.bestStreak
import dev.sito8943.focushabit.core.model.streak
import dev.sito8943.focushabit.core.model.weekdayCounts
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitDetailUiState(
    /** Null while loading, or once the habit is deleted. */
    val habit: Habit?,
    val today: LocalDate,
    val loggedDays: Set<LocalDate>,
    val streak: Int,
    val bestStreak: Int,
    /** Last 12 weeks, Monday first. */
    val weekdays: Map<DayOfWeek, Int>,
    val loading: Boolean,
) {
    val doneToday: Boolean get() = today in loggedDays
}

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
    codeStore: SyncCodeStore,
    private val dates: DateState,
) : ViewModel() {

    val state: StateFlow<HabitDetailUiState> = codeStore.code
        .flatMapLatest { code ->
            combine(repository.observeHabits(code), repository.observeLogs(code), dates.today) { habits, logs, today ->
                val days = logs.filter { it.habitId == habitId }.mapTo(HashSet()) { it.date }
                HabitDetailUiState(
                    habit = habits.firstOrNull { it.id == habitId },
                    today = today,
                    loggedDays = days,
                    streak = streak(days, today),
                    bestStreak = bestStreak(days),
                    weekdays = weekdayCounts(days, today),
                    loading = false,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HabitDetailUiState(null, dates.today.value, emptySet(), 0, 0, emptyMap(), loading = true),
        )

    fun toggleToday() {
        viewModelScope.launch { repository.toggleLog(habitId, dates.today.value) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            onDone()
        }
    }
}
