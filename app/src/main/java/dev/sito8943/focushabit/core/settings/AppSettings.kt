package dev.sito8943.focushabit.core.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val storage: String, val tag: String?) {
    System("system", null),
    Spanish("es", "es"),
    English("en", "en"),
}

enum class ThemeMode(val storage: String) { System("system"), Light("light"), Dark("dark") }

enum class WeekStart(val storage: String) { Sunday("sunday"), Monday("monday") }

/** Device preferences (not synced). */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(read(KEY_LANGUAGE, AppLanguage.entries, AppLanguage.System) { it.storage })
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _theme = MutableStateFlow(read(KEY_THEME, ThemeMode.entries, ThemeMode.System) { it.storage })
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _weekStart = MutableStateFlow(read(KEY_WEEK_START, WeekStart.entries, WeekStart.Sunday) { it.storage })
    val weekStart: StateFlow<WeekStart> = _weekStart.asStateFlow()

    fun setLanguage(value: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, value.storage).apply()
        _language.value = value
    }

    fun setTheme(value: ThemeMode) {
        prefs.edit().putString(KEY_THEME, value.storage).apply()
        _theme.value = value
    }

    fun setWeekStart(value: WeekStart) {
        prefs.edit().putString(KEY_WEEK_START, value.storage).apply()
        _weekStart.value = value
    }

    private fun <T> read(key: String, entries: List<T>, default: T, storage: (T) -> String): T {
        val stored = prefs.getString(key, null) ?: return default
        return entries.firstOrNull { storage(it) == stored } ?: default
    }

    companion object {
        private const val PREFS = "focus_habit_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_WEEK_START = "week_start"

        /**
         * The activity's context in the chosen language. Read straight from the
         * preferences because it runs in `attachBaseContext`, before Compose.
         */
        fun localized(base: Context): Context {
            val stored = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            val tag = AppLanguage.entries.firstOrNull { it.storage == stored }?.tag
            if (tag == null) {
                // Back to the phone's language, also for dates formatted with Locale.getDefault().
                Locale.setDefault(base.resources.configuration.locales[0])
                return base
            }
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale) // dates and month names follow the app language too
            val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(config)
        }
    }
}
