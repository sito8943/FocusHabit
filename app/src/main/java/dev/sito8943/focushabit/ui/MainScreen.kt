package dev.sito8943.focushabit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.di.AppContainer
import dev.sito8943.focushabit.core.sync.SyncStatus
import dev.sito8943.focushabit.feature.calendar.CalendarScreen
import dev.sito8943.focushabit.feature.habit.HabitDetailScreen
import dev.sito8943.focushabit.feature.recovery.RecoverySheet
import dev.sito8943.focushabit.feature.today.TodayScreen
import dev.sito8943.focushabit.ui.theme.Alice

private enum class Tab { Today, Calendar }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(container: AppContainer) {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var showRecovery by rememberSaveable { mutableStateOf(false) }
    var openHabit by rememberSaveable { mutableStateOf<Long?>(null) }

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

    openHabit?.let { habitId ->
        HabitDetailRoute(container, habitId, onClose = { openHabit = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.app_name),
                            fontFamily = Alice,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
                actions = {
                    SyncButton(syncStatus) { container.syncCoordinator.refresh() }
                    IconButton(onClick = { showRecovery = true }) {
                        Icon(Icons.Filled.Key, contentDescription = stringResource(R.string.recovery_open))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Today,
                    onClick = { tab = Tab.Today },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_today)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.Calendar,
                    onClick = { tab = Tab.Calendar },
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_calendar)) },
                )
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
                        onAdd = today::addHabit,
                        onSelectDay = today::select,
                        onBackToToday = today::backToToday,
                    )
                }

                Tab.Calendar -> {
                    val state by calendar.state.collectAsStateWithLifecycle()
                    CalendarScreen(
                        state = state,
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

    if (showRecovery) {
        val code by recovery.code.collectAsStateWithLifecycle()
        val state by recovery.state.collectAsStateWithLifecycle()
        RecoverySheet(
            code = code,
            state = state,
            onInput = recovery::onInput,
            onApply = recovery::apply,
            onShowRestore = recovery::showRestore,
            onDismiss = {
                showRecovery = false
                recovery.reset()
            },
        )
    }
}

@Composable
private fun HabitDetailRoute(container: AppContainer, habitId: Long, onClose: () -> Unit) {
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
            Icon(Icons.Filled.Sync, contentDescription = stringResource(R.string.sync_now))
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
        SyncStatus.NotConfigured -> stringResource(R.string.sync_not_configured)
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
