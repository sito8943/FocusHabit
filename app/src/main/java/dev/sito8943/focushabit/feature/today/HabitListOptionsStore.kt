package dev.sito8943.focushabit.feature.today

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Remembers the Today sort and filters on this device (not synced). */
class HabitListOptionsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _options = MutableStateFlow(read())
    val options: StateFlow<HabitListOptions> = _options.asStateFlow()

    fun set(value: HabitListOptions) {
        prefs.edit()
            .putString(KEY_SORT, value.sort.name)
            .putString(KEY_STATUS, value.status.name)
            .putStringSet(KEY_FREQUENCIES, value.frequencies.mapTo(HashSet()) { it.name })
            // A category is never blank, so "" stands for "no category".
            .putStringSet(KEY_CATEGORIES, value.categories.mapTo(HashSet()) { it ?: NO_CATEGORY })
            .apply()
        _options.value = value
    }

    private fun read(): HabitListOptions = HabitListOptions(
        sort = HabitSort.entries.firstOrNull { it.name == prefs.getString(KEY_SORT, null) } ?: HabitSort.Default,
        status = StatusFilter.entries.firstOrNull { it.name == prefs.getString(KEY_STATUS, null) } ?: StatusFilter.All,
        frequencies = prefs.getStringSet(KEY_FREQUENCIES, null).orEmpty()
            .mapNotNullTo(HashSet()) { name -> FrequencyKind.entries.firstOrNull { it.name == name } },
        categories = prefs.getStringSet(KEY_CATEGORIES, null).orEmpty()
            .mapTo(HashSet()) { it.takeIf { it != NO_CATEGORY } },
    )

    private companion object {
        const val PREFS = "focus_habit_list"
        const val KEY_SORT = "sort"
        const val KEY_STATUS = "status"
        const val KEY_FREQUENCIES = "frequencies"
        const val KEY_CATEGORIES = "categories"
        const val NO_CATEGORY = ""
    }
}
