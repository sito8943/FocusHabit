package dev.sito8943.focushabit.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.di.DateState
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.model.HabitSchedule
import dev.sito8943.focushabit.core.model.PeriodProgress
import dev.sito8943.focushabit.core.model.Streak
import dev.sito8943.focushabit.core.model.dayCompletion
import dev.sito8943.focushabit.core.model.perfectDaysStreak
import dev.sito8943.focushabit.core.settings.AppSettings
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitRow(
    val id: Long,
    val name: String,
    val color: String,
    val icon: String?,
    val kind: HabitKind,
    /** Build: done on the selected day. Quit: no relapse that day. */
    val done: Boolean,
    /** Counts in the day ring: due that day, or a flexible habit done that day. */
    val required: Boolean,
    val streak: Streak,
    /** N per week/month habits: progress in the selected day's period. */
    val period: PeriodProgress?,
    val goal: Int?,
    val goalProgress: Int,
    /** Optional time of day; timed habits come first, earliest first. */
    val time: LocalTime? = null,
    val frequency: FrequencyKind = FrequencyKind.Daily,
    val category: String? = null,
)

data class TodayUiState(
    val date: LocalDate,
    val today: LocalDate,
    /** Due on the selected day, or still open (flexible quota). */
    val habits: List<HabitRow>,
    /** Not expected that day (other weekdays, not due yet, quota met). */
    val others: List<HabitRow>,
    /** Share of due habits done per day, for the strip and the dots. */
    val completion: Map<LocalDate, Float>,
    /** Consecutive days with everything due done. */
    val perfectStreak: Int,
    val hasHabits: Boolean,
    val loading: Boolean,
    val options: HabitListOptions = HabitListOptions(),
    /** Categories in use (sorted), for the filter chips. */
    val categories: List<String> = emptyList(),
    /** True if some habit has no category (offers a "No category" chip). */
    val hasUncategorized: Boolean = false,
) {
    /** [habits] and [others] after the user's sort and filters; progress still counts all. */
    val visibleHabits: List<HabitRow> get() = options.apply(habits)
    val visibleOthers: List<HabitRow> get() = options.apply(others)

    val isToday: Boolean get() = date == today
    private val counted get() = habits.filter { it.required }
    val doneCount: Int get() = counted.count { it.done }
    val dueCount: Int get() = counted.size
    val progress: Float get() = if (dueCount == 0) 0f else doneCount.toFloat() / dueCount
}

private const val HISTORY_DAYS = 366L
private const val FUTURE_DAYS = 7L

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: HabitRepository,
    private val codeStore: SyncCodeStore,
    private val dates: DateState,
    private val settings: AppSettings,
) : ViewModel() {

    private val options = MutableStateFlow(HabitListOptions())

    val state: StateFlow<TodayUiState> = codeStore.code
        .flatMapLatest { code ->
            combine(
                repository.observeHabits(code),
                repository.observeLogs(code),
                dates.selected,
                dates.today,
                settings.weekStart,
            ) { habits, logs, selected, today, weekStart ->
                val logsByHabit = logs.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }
                val schedules = habits.map { HabitSchedule(it, logsByHabit[it.id].orEmpty(), weekStart.dayOfWeek) }
                val completion = buildMap {
                    var day = today.minusDays(HISTORY_DAYS)
                    while (day <= today.plusDays(FUTURE_DAYS)) {
                        dayCompletion(schedules, day)?.let { put(day, it) }
                        day = day.plusDays(1)
                    }
                }
                val rows = schedules.sortedWith(compareBy(nullsLast()) { it.habit.time }).map { schedule ->
                    val habit = schedule.habit
                    val row = HabitRow(
                        id = habit.id,
                        name = habit.name,
                        color = habit.color,
                        icon = habit.icon,
                        kind = habit.kind,
                        done = schedule.isDone(selected),
                        required = schedule.isDue(selected),
                        streak = schedule.streak(today),
                        period = schedule.periodProgress(selected),
                        goal = habit.goal,
                        goalProgress = schedule.goalProgress(today),
                        time = habit.time,
                        frequency = FrequencyKind.of(habit.frequency),
                        category = habit.category,
                    )
                    row to (schedule.isOpen(selected) || row.done && habit.kind == HabitKind.Build)
                }
                TodayUiState(
                    date = selected,
                    today = today,
                    habits = rows.filter { it.second }.map { it.first },
                    others = rows.filterNot { it.second }.map { it.first },
                    completion = completion,
                    perfectStreak = perfectDaysStreak({ completion[it] }, today),
                    hasHabits = habits.isNotEmpty(),
                    loading = false,
                    categories = habits.mapNotNull { it.category }.distinct().sortedBy { it.lowercase() },
                    hasUncategorized = habits.any { it.category == null },
                )
            }.flowOn(Dispatchers.Default)
        }
        .combine(options) { state, options -> state.copy(options = options) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayUiState(
                date = dates.selected.value,
                today = dates.today.value,
                habits = emptyList(),
                others = emptyList(),
                completion = emptyMap(),
                perfectStreak = 0,
                hasHabits = false,
                loading = true,
            ),
        )

    /** Build: mark/unmark done. Quit: mark/unmark a relapse. */
    fun toggle(habitId: Long) {
        val date = state.value.date
        viewModelScope.launch { repository.toggleLog(habitId, date) }
    }

    fun setOptions(value: HabitListOptions) {
        options.value = value
    }

    fun select(day: LocalDate) {
        dates.selected.value = day
    }

    fun backToToday() {
        dates.selected.value = dates.today.value
    }
}
