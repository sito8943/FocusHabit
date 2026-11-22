package dev.sito8943.focushabit.core.settings

import android.content.Context
import dev.sito8943.focushabit.core.model.SyncCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The active recovery code. Generated on first launch, like the web app
 * (localStorage `habit-sync-code`), and replaced only by restoring another one.
 */
class SyncCodeStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _code = MutableStateFlow(loadOrCreate())
    val code: StateFlow<String> = _code.asStateFlow()

    fun set(code: String) {
        require(SyncCode.isValid(code)) { "Invalid sync code" }
        prefs.edit().putString(KEY, code).apply()
        _code.value = code
    }

    private fun loadOrCreate(): String {
        val stored = SyncCode.normalize(prefs.getString(KEY, null).orEmpty())
        if (SyncCode.isValid(stored)) return stored
        val generated = SyncCode.generate()
        prefs.edit().putString(KEY, generated).apply()
        return generated
    }

    private companion object {
        const val PREFS = "focus_habit"
        const val KEY = "habit-sync-code"
    }
}
