package dev.sito8943.focushabit.core.di

import android.content.Context
import dev.sito8943.focushabit.BuildConfig
import dev.sito8943.focushabit.core.data.FocusHabitDatabase
import dev.sito8943.focushabit.core.data.HabitRepository
import dev.sito8943.focushabit.core.network.SupabaseRest
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import dev.sito8943.focushabit.core.sync.HabitApi
import dev.sito8943.focushabit.core.sync.SyncCoordinator
import dev.sito8943.focushabit.core.sync.SyncEngine
import dev.sito8943.focushabit.feature.calendar.CalendarViewModel
import dev.sito8943.focushabit.feature.form.HabitFormViewModel
import dev.sito8943.focushabit.feature.habit.HabitDetailViewModel
import dev.sito8943.focushabit.feature.recovery.RecoveryViewModel
import dev.sito8943.focushabit.feature.today.TodayViewModel
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

/**
 * Day shown on Today and highlighted on Calendar, shared by both like the
 * web's global `selectedDate`. [today] is refreshed when the app resumes so
 * the app does not stay on yesterday after midnight.
 */
class DateState {
    val today = MutableStateFlow(LocalDate.now())
    val selected = MutableStateFlow(today.value)

    fun onResume() {
        val previous = today.value
        val current = LocalDate.now()
        if (current == previous) return
        today.value = current
        // Was following "today": keep following it.
        if (selected.value == previous) selected.value = current
    }
}

/** The single place where dependencies are built. No Hilt, like SitoWallet. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        // PostgREST adds embedded resources (`habits`) and new columns over time.
        ignoreUnknownKeys = true
    }

    private val database: FocusHabitDatabase by lazy { FocusHabitDatabase.build(appContext) }

    private val rest by lazy {
        SupabaseRest(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, json)
    }

    val codeStore: SyncCodeStore by lazy { SyncCodeStore(appContext) }

    val repository: HabitRepository by lazy { HabitRepository(database) }

    val dates = DateState()

    val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(
            engine = SyncEngine(database, HabitApi(rest, json)),
            habits = database.habits(),
            code = codeStore.code,
            isConfigured = rest.isConfigured,
            scope = applicationScope,
        )
    }

    fun createTodayViewModel() = TodayViewModel(repository, codeStore, dates)

    fun createCalendarViewModel() = CalendarViewModel(repository, codeStore, dates)

    fun createHabitDetailViewModel(habitId: Long) =
        HabitDetailViewModel(habitId, repository, codeStore, dates)

    fun createHabitFormViewModel(habitId: Long?) = HabitFormViewModel(habitId, repository, codeStore)

    fun createRecoveryViewModel() = RecoveryViewModel(codeStore, syncCoordinator)
}
