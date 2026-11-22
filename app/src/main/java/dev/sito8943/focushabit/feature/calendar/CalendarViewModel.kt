package dev.sito8943.focushabit.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.di.DateState
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class DayAppearance { Selected, Logged, Today, Normal }

data class CalendarUiState(
    val month: YearMonth,
    val selected: LocalDate,
    val today: LocalDate,
    val loggedDays: Set<LocalDate>,
) {
    /** Same precedence as the web's `resolveCalendarDayAppearance`. */
    fun appearance(day: LocalDate): DayAppearance = when (day) {
        selected -> DayAppearance.Selected
        in loggedDays -> DayAppearance.Logged
        today -> DayAppearance.Today
        else -> DayAppearance.Normal
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    repository: HabitRepository,
    codeStore: SyncCodeStore,
    private val dates: DateState,
) : ViewModel() {

    /** Opens on the month of the selected day; the web only showed the current month. */
    private val month = MutableStateFlow(YearMonth.from(dates.selected.value))

    val state: StateFlow<CalendarUiState> = combine(
        codeStore.code.flatMapLatest { code ->
            repository.observeLogs(code).map { logs -> logs.mapTo(HashSet()) { it.date } }
        },
        month,
        dates.selected,
        dates.today,
    ) { logged, month, selected, today ->
        CalendarUiState(month, selected, today, logged)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(month.value, dates.selected.value, dates.today.value, emptySet()),
    )

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun select(day: LocalDate) {
        dates.selected.value = day
    }
}
