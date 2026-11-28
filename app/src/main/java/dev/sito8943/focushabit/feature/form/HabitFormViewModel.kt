package dev.sito8943.focushabit.feature.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.core.model.Frequency
import dev.sito8943.focushabit.core.model.Habit
import dev.sito8943.focushabit.core.model.HabitColors
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FrequencyType { Daily, Weekdays, EveryNDays, PerWeek, PerMonth }

data class HabitFormState(
    val loading: Boolean = false,
    val name: String = "",
    val icon: String? = null,
    val color: String = HabitColors.default,
    val kind: HabitKind = HabitKind.Build,
    val frequencyType: FrequencyType = FrequencyType.Daily,
    /** Monday–Friday until she picks other days. */
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.take(5).toSet(),
    val everyDays: Int = 2,
    val perWeek: Int = 3,
    val perMonth: Int = 10,
    val goalEnabled: Boolean = false,
    val goal: Int = 30,
    /** Null: no time of day. */
    val time: LocalTime? = null,
    /** Null: no category. */
    val categoryId: Long? = null,
    /** The user's categories, offered as chips. */
    val categories: List<Category> = emptyList(),
    val saving: Boolean = false,
) {
    val canSave: Boolean
        get() = !loading && !saving && name.isNotBlank() &&
            (frequencyType != FrequencyType.Weekdays || weekdays.isNotEmpty() || kind == HabitKind.Quit)

    fun frequency(): Frequency = when (frequencyType) {
        FrequencyType.Daily -> Frequency.Daily
        FrequencyType.Weekdays -> Frequency.Weekdays(weekdays)
        FrequencyType.EveryNDays -> Frequency.EveryNDays(everyDays)
        FrequencyType.PerWeek -> Frequency.TimesPerWeek(perWeek)
        FrequencyType.PerMonth -> Frequency.TimesPerMonth(perMonth)
    }
}

class HabitFormViewModel(
    private val habitId: Long?,
    private val repository: HabitRepository,
    private val codeStore: SyncCodeStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HabitFormState(loading = habitId != null))
    val state: StateFlow<HabitFormState> = _state.asStateFlow()

    val isEdit: Boolean = habitId != null

    init {
        if (habitId != null) {
            viewModelScope.launch {
                val habit = repository.getHabit(habitId)
                _state.update { (habit?.toForm() ?: HabitFormState()).copy(categories = it.categories) }
            }
        }
        viewModelScope.launch {
            repository.observeCategories(codeStore.code.value).collect { categories ->
                _state.update { form ->
                    // A category deleted meanwhile is no longer selectable. One just created
                    // (selected before this list caught up) is kept: it was never in the old list.
                    val deleted = { id: Long -> form.categories.any { it.id == id } && categories.none { it.id == id } }
                    form.copy(categories = categories, categoryId = form.categoryId?.takeUnless(deleted))
                }
            }
        }
    }

    fun update(change: (HabitFormState) -> HabitFormState) = _state.update(change)

    fun save(onSaved: () -> Unit) {
        val form = _state.value
        if (!form.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.saveHabit(
                code = codeStore.code.value,
                id = habitId,
                draft = Habit(
                    id = habitId ?: 0,
                    name = form.name.trim(),
                    color = form.color,
                    icon = form.icon,
                    kind = form.kind,
                    frequency = form.frequency(),
                    goal = form.goal.takeIf { form.goalEnabled },
                    time = form.time,
                    categoryId = form.categoryId,
                ),
            )
            onSaved()
        }
    }
}

private fun Habit.toForm(): HabitFormState {
    val base = HabitFormState(
        name = name,
        icon = icon,
        color = color,
        kind = kind,
        goalEnabled = goal != null,
        goal = goal ?: 30,
        time = time,
        categoryId = categoryId,
    )
    return when (val f = frequency) {
        Frequency.Daily -> base
        is Frequency.Weekdays -> base.copy(frequencyType = FrequencyType.Weekdays, weekdays = f.days)
        is Frequency.EveryNDays -> base.copy(frequencyType = FrequencyType.EveryNDays, everyDays = f.days)
        is Frequency.TimesPerWeek -> base.copy(frequencyType = FrequencyType.PerWeek, perWeek = f.times)
        is Frequency.TimesPerMonth -> base.copy(frequencyType = FrequencyType.PerMonth, perMonth = f.times)
    }
}
