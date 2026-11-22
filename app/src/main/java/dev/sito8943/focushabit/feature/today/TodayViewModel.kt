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
    val isToday: Boolean,
    val habits: List<HabitRow>,
    val loading: Boolean,
) {
    val doneCount: Int get() = habits.count { it.done }
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
                TodayUiState(
                    date = selected,
                    isToday = selected == today,
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
                    loading = false,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayUiState(dates.selected.value, isToday = true, habits = emptyList(), loading = true),
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

    fun delete(habitId: Long) {
        viewModelScope.launch { repository.deleteHabit(habitId) }
    }

    fun backToToday() {
        dates.selected.value = dates.today.value
    }

    fun shiftDay(days: Long) {
        dates.selected.value = dates.selected.value.plusDays(days)
    }
}
