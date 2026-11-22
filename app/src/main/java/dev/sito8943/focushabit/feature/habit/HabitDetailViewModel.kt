package dev.sito8943.focushabit.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.di.DateState
import dev.sito8943.focushabit.core.model.Habit
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.model.HabitSchedule
import dev.sito8943.focushabit.core.model.Streak
import dev.sito8943.focushabit.core.model.StreakUnit
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
    /** Build: days done. Quit: days clean. */
    val goodDays: Set<LocalDate>,
    val streak: Streak,
    val goalProgress: Int,
    /** Build: done per weekday. Quit: relapses per weekday. Last 12 weeks, Monday first. */
    val weekdays: Map<DayOfWeek, Int>,
    /** Build: done today. Quit: relapsed today. */
    val loggedToday: Boolean,
    val loading: Boolean,
)

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
                val habit = habits.firstOrNull { it.id == habitId }
                    ?: return@combine HabitDetailUiState(null, today, emptySet(), Streak(0, 0, StreakUnit.Days), 0, emptyMap(), false, loading = false)
                val schedule = HabitSchedule(habit, days)
                val good = if (habit.kind == HabitKind.Build) {
                    days
                } else {
                    generateSequence(schedule.start) { it.plusDays(1) }.takeWhile { it <= today }.filter(schedule::isDone).toSet()
                }
                HabitDetailUiState(
                    habit = habit,
                    today = today,
                    goodDays = good,
                    streak = schedule.streak(today),
                    goalProgress = schedule.goalProgress(today),
                    weekdays = weekdayCounts(days, today),
                    loggedToday = today in days,
                    loading = false,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HabitDetailUiState(null, dates.today.value, emptySet(), Streak(0, 0, StreakUnit.Days), 0, emptyMap(), false, loading = true),
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
