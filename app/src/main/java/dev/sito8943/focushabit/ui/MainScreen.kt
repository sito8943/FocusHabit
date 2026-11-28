package dev.sito8943.focushabit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.core.di.AppContainer
import dev.sito8943.focushabit.core.sync.SyncStatus
import dev.sito8943.focushabit.feature.calendar.CalendarScreen
import dev.sito8943.focushabit.feature.categories.CategoriesScreen
import dev.sito8943.focushabit.feature.form.HabitFormScreen
import dev.sito8943.focushabit.feature.recovery.RecoveryViewModel
import dev.sito8943.focushabit.feature.settings.SettingsScreen
import androidx.activity.compose.LocalActivity
import dev.sito8943.focushabit.feature.habit.HabitDetailScreen
import dev.sito8943.focushabit.feature.recovery.RecoverySheet
import dev.sito8943.focushabit.feature.today.TodayScreen
import dev.sito8943.focushabit.ui.patterns.TitleText

private enum class Tab { Today, Calendar }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(container: AppContainer) {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var showRecovery by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    /** Over whatever opened it (Settings or the habit form), which stays below. */
    var showCategories by rememberSaveable { mutableStateOf(false) }
    var openHabit by rememberSaveable { mutableStateOf<Long?>(null) }
    /** Habit form: [NEW_HABIT] to create, an id to edit, null when closed. */
    var form by rememberSaveable { mutableStateOf<Long?>(null) }
    /** Category created from the form's "Manage": selected for that habit on return. */
    var createdCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    /**
     * Bumped on every opening, and part of the form ViewModel's key: each
     * opening gets a fresh form. With a fixed key the activity-scoped ViewModel
     * was reused, still holding the last habit and `saving = true`, so the
     * next "+" showed old data and could not save. rememberSaveable keeps the
     * draft across rotation.
     */
    var formSession by rememberSaveable { mutableIntStateOf(0) }
    fun openForm(target: Long) {
        formSession++
        form = target
    }

    val today = viewModel { container.createTodayViewModel() }
    val calendar = viewModel { container.createCalendarViewModel() }
    val recovery = viewModel { container.createRecoveryViewModel() }
    val syncStatus by container.syncCoordinator.status.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        container.dates.onResume()
        container.syncCoordinator.refresh()
        onPauseOrDispose { }
    }

    BackHandler(enabled = tab != Tab.Today) { tab = Tab.Today }

    if (showCategories) {
        CategoriesRoute(
            container,
            onClose = { showCategories = false },
            // Only when opened from the habit form: she expects a new category to be this habit's.
            onCreated = if (form != null) { id -> createdCategory = id } else null,
        )
        return
    }
    if (showSettings) {
        SettingsRoute(
            container,
            onClose = { showSettings = false },
            onRecoveryCode = { showRecovery = true },
            onCategories = { showCategories = true },
        )
        RecoveryHost(showRecovery, recovery) { showRecovery = false }
        return
    }
    form?.let { target ->
        HabitFormRoute(
            container,
            target.takeIf { it != NEW_HABIT },
            formSession,
            onClose = { form = null },
            onManageCategories = { showCategories = true },
            createdCategory = createdCategory,
            onCreatedCategoryApplied = { createdCategory = null },
        )
        return
    }
    openHabit?.let { habitId ->
        HabitDetailRoute(container, habitId, onClose = { openHabit = null }, onEdit = { openForm(habitId) })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TitleText(
                            stringResource(R.string.app_name),
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                actions = {
                    SyncButton(syncStatus) { container.syncCoordinator.refresh() }
                    IconButton(onClick = { showSettings = true }) {
                        FaIcon(Fa.Settings, contentDescription = stringResource(R.string.settings_title), Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            // Same bar as SitoWallet: full-width Material 3, label under the icon,
            // accent on the active tab. The centre is the filled "+".
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TabItem(Tab.Today, tab, Fa.Today, R.string.tab_today) { tab = it }
                Box(Modifier.weight(1f).align(Alignment.CenterVertically), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = { openForm(NEW_HABIT) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            FaIcon(Fa.Plus, stringResource(R.string.habit_new), Modifier.size(24.dp))
                        }
                    }
                }
                TabItem(Tab.Calendar, tab, Fa.Calendar, R.string.tab_calendar) { tab = it }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // The bottom bar's inset is already in `padding`; the keyboard is not:
        // without imePadding the add form stays hidden under it (edge-to-edge).
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            SyncBanner(syncStatus)
            when (tab) {
                Tab.Today -> {
                    val state by today.state.collectAsStateWithLifecycle()
                    TodayScreen(
                        state = state,
                        onToggle = today::toggle,
                        onOpen = { openHabit = it },
                        onSelectDay = today::select,
                        onBackToToday = today::backToToday,
                        onOptions = today::setOptions,
                    )
                }

                Tab.Calendar -> {
                    val state by calendar.state.collectAsStateWithLifecycle()
                    val weekStart by container.settings.weekStart.collectAsStateWithLifecycle()
                    CalendarScreen(
                        state = state,
                        weekStart = weekStart.dayOfWeek,
                        onPrevious = calendar::previousMonth,
                        onNext = calendar::nextMonth,
                        onSelect = { day ->
                            calendar.select(day)
                            tab = Tab.Today
                        },
                    )
                }
            }
        }
    }

    RecoveryHost(showRecovery, recovery) { showRecovery = false }
}

@Composable
private fun SettingsRoute(
    container: AppContainer,
    onClose: () -> Unit,
    onRecoveryCode: () -> Unit,
    onCategories: () -> Unit,
) {
    val settings = container.settings
    val language by settings.language.collectAsStateWithLifecycle()
    val theme by settings.theme.collectAsStateWithLifecycle()
    val weekStart by settings.weekStart.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    BackHandler(onBack = onClose)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        SettingsScreen(
            language = language,
            theme = theme,
            weekStart = weekStart,
            onLanguage = {
                if (it != language) {
                    settings.setLanguage(it)
                    activity?.recreate() // strings and dates are resolved per activity context
                }
            },
            onTheme = settings::setTheme,
            onWeekStart = settings::setWeekStart,
            onRecoveryCode = onRecoveryCode,
            onCategories = onCategories,
            onBack = onClose,
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@Composable
private fun CategoriesRoute(container: AppContainer, onClose: () -> Unit, onCreated: ((Long) -> Unit)?) {
    val vm = viewModel { container.createCategoriesViewModel() }
    DisposableEffect(vm, onCreated) {
        vm.onCreated = onCreated
        onDispose { vm.onCreated = null }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onClose)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        CategoriesScreen(
            state = state,
            onSave = vm::save,
            onDelete = vm::delete,
            onAddPrefab = vm::addPrefab,
            onBack = onClose,
            modifier = Modifier.safeDrawingPadding().imePadding(),
        )
    }
}

@Composable
private fun RecoveryHost(show: Boolean, recovery: RecoveryViewModel, onClose: () -> Unit) {
    if (show) {
        val code by recovery.code.collectAsStateWithLifecycle()
        val state by recovery.state.collectAsStateWithLifecycle()
        RecoverySheet(
            code = code,
            state = state,
            onInput = recovery::onInput,
            onApply = recovery::apply,
            onShowRestore = recovery::showRestore,
            onDismiss = {
                onClose()
                recovery.reset()
            },
        )
    }
}

private const val NEW_HABIT = -1L

@Composable
private fun RowScope.TabItem(target: Tab, current: Tab, icon: String, label: Int, onSelect: (Tab) -> Unit) {
    val selected = target == current
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(target) },
        icon = { FaIcon(icon, contentDescription = null, Modifier.size(22.dp)) },
        label = { Text(stringResource(label), style = MaterialTheme.typography.labelMedium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.tertiary,
            selectedTextColor = MaterialTheme.colorScheme.tertiary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    )
}

@Composable
private fun HabitFormRoute(
    container: AppContainer,
    habitId: Long?,
    session: Int,
    onClose: () -> Unit,
    onManageCategories: () -> Unit,
    createdCategory: Long?,
    onCreatedCategoryApplied: () -> Unit,
) {
    val vm = viewModel(key = "form-${habitId ?: "new"}-$session") { container.createHabitFormViewModel(habitId) }
    LaunchedEffect(createdCategory) {
        createdCategory?.let { id ->
            vm.update { it.copy(categoryId = id) }
            onCreatedCategoryApplied()
        }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onClose)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        HabitFormScreen(
            state = state,
            isEdit = vm.isEdit,
            onChange = vm::update,
            onSave = { vm.save(onClose) },
            onClose = onClose,
            onManageCategories = onManageCategories,
            modifier = Modifier.safeDrawingPadding().imePadding(),
        )
    }
}

@Composable
private fun HabitDetailRoute(container: AppContainer, habitId: Long, onClose: () -> Unit, onEdit: () -> Unit) {
    val detail = viewModel(key = "habit-$habitId") { container.createHabitDetailViewModel(habitId) }
    val state by detail.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onClose)
    // Deleted here or by a sync from another device.
    LaunchedEffect(state.loading, state.habit) {
        if (!state.loading && state.habit == null) onClose()
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        HabitDetailScreen(
            state = state,
            onBack = onClose,
            onToggleToday = detail::toggleToday,
            onDelete = { detail.delete(onClose) },
            onEdit = onEdit,
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@Composable
private fun SyncButton(status: SyncStatus, onClick: () -> Unit) {
    if (status == SyncStatus.NotConfigured) return
    IconButton(onClick = onClick, enabled = status != SyncStatus.Syncing) {
        if (status == SyncStatus.Syncing) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            FaIcon(Fa.Sync, contentDescription = stringResource(R.string.sync_now))
        }
    }
}

/** Only when something is wrong. Offline is not an error: changes stay on the phone. */
@Composable
private fun SyncBanner(status: SyncStatus) {
    val text = when (status) {
        is SyncStatus.Failed ->
            if (status.offline) stringResource(R.string.sync_offline)
            else stringResource(R.string.sync_failed, status.message)
        // Local-only build: nothing to warn about, data lives on the phone by design.
        else -> return
    }
    // Informational, not an error: data is safe on the phone.
    val offline = status == SyncStatus.NotConfigured || (status is SyncStatus.Failed && status.offline)
    Surface(
        color = if (offline) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (offline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
