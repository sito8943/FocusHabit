package dev.sito8943.focushabit.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.model.PeriodProgress
import dev.sito8943.focushabit.core.model.Streak
import dev.sito8943.focushabit.core.model.StreakUnit
import dev.sito8943.focushabit.ui.patterns.CategoryLabel
import dev.sito8943.focushabit.ui.patterns.Celebration
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.HabitTile
import dev.sito8943.focushabit.ui.patterns.HeatLegend
import dev.sito8943.focushabit.ui.patterns.Heatmap
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.patterns.TitleText
import dev.sito8943.focushabit.ui.theme.FocusColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    state: TodayUiState,
    onToggle: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onBackToToday: () -> Unit,
    onOptions: (HabitListOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOptions by rememberSaveable { mutableStateOf(false) }
    val visibleHabits = state.visibleHabits
    val visibleOthers = state.visibleOthers
    // Celebrate the moment the last habit of today gets checked, not on opening
    // a day that was already complete.
    var celebrate by remember { mutableStateOf(false) }
    var previous by remember { mutableStateOf<Pair<LocalDate, Boolean>?>(null) }
    val allDone = state.dueCount > 0 && state.doneCount == state.dueCount
    LaunchedEffect(state.loading, state.date, allDone) {
        if (state.loading) return@LaunchedEffect
        val before = previous
        if (before != null && before.first == state.date && !before.second && allDone && state.isToday) {
            celebrate = true
        }
        previous = state.date to allDone
    }

    val listState = rememberLazyListState()
    // The day strip (item 1) sticks on top; once content scrolls under it, it shrinks.
    val stripCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 ||
                (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 0)
        }
    }
    val collapse by animateFloatAsState(if (stripCollapsed) 1f else 0f, label = "strip")

    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") { DayHeader(state, onBackToToday) }
            stickyHeader(key = "strip") {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 3.dp * collapse,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WeekStrip(state, onSelectDay, collapse, Modifier.padding(vertical = 4.dp))
                }
            }
            item(key = "hero") { ProgressAccordion(state, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
            item(key = "label") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(stringResource(R.string.today_habits), Modifier.weight(1f))
                    if (state.hasHabits) {
                        ListOptionsButton(state.options) { showOptions = true }
                    }
                }
            }
            if (visibleHabits.isEmpty() && !state.loading) {
                item(key = "empty") {
                    Text(
                        text = stringResource(
                            when {
                                !state.hasHabits -> R.string.today_empty
                                state.habits.isNotEmpty() -> R.string.today_no_match
                                else -> R.string.today_nothing_due
                            },
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.grouped) {
                groupByCategory(visibleHabits, state.categories).forEach { (category, group) ->
                    item(key = "group-${category?.id ?: "none"}") {
                        CategoryHeader(category, group, Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp).animateItem())
                    }
                    items(group, key = { it.id }) { habit ->
                        HabitItem(
                            habit = habit,
                            onToggle = { onToggle(habit.id) },
                            onOpen = { onOpen(habit.id) },
                            showCategory = false,
                            modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
                        )
                    }
                }
            } else {
                items(visibleHabits, key = { it.id }) { habit ->
                    HabitItem(
                        habit = habit,
                        onToggle = { onToggle(habit.id) },
                        onOpen = { onOpen(habit.id) },
                        modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
                    )
                }
            }
            if (visibleOthers.isNotEmpty()) {
                item(key = "others-label") {
                    SectionLabel(
                        stringResource(R.string.today_others),
                        Modifier.padding(start = 20.dp, top = 12.dp).animateItem(),
                    )
                }
                items(visibleOthers, key = { it.id }) { habit ->
                    HabitItem(
                        habit = habit,
                        onToggle = { onToggle(habit.id) },
                        onOpen = { onOpen(habit.id) },
                        modifier = Modifier.padding(horizontal = 16.dp).alpha(0.6f).animateItem(),
                    )
                }
            }
        }
    }
    if (showOptions) {
        HabitListOptionsSheet(
            state = state,
            onChange = onOptions,
            onDismiss = { showOptions = false },
        )
    }
    Celebration(
        visible = celebrate,
        title = stringResource(R.string.celebrate_title),
        message = stringResource(R.string.celebrate_message),
        onFinished = { celebrate = false },
    )
    }
}

@Composable
private fun DayHeader(state: TodayUiState, onBackToToday: () -> Unit) {
    val locale = Locale.getDefault()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            SectionLabel(state.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)))
            TitleText(
                text = when (state.date) {
                    state.today -> stringResource(greeting(LocalTime.now().hour))
                    state.today.minusDays(1) -> stringResource(R.string.day_yesterday)
                    state.today.plusDays(1) -> stringResource(R.string.day_tomorrow)
                    else -> state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                },
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!state.isToday) {
            // A real button (tonal pill with icon), not loose text: it is the way back.
            FilledTonalButton(
                onClick = onBackToToday,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                FaIcon(Fa.BackToToday, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.today_back_to_today), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun greeting(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..19 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}

private const val PAST_DAYS = 59L
private const val FUTURE_DAYS = 7L

/** Horizontal day picker: green = done (stronger when everything is), navy = selected, gold ring = today. */
@Composable
private fun WeekStrip(
    state: TodayUiState,
    onSelect: (LocalDate) -> Unit,
    /** 0 = full size, 1 = compact (number only). */
    collapse: Float,
    modifier: Modifier = Modifier,
) {
    val first = state.today.minusDays(PAST_DAYS)
    val days = (0..PAST_DAYS + FUTURE_DAYS).map { first.plusDays(it) }
    val selectedIndex = days.indexOf(state.date)
    // Start already on the selected day: an animated scroll launched on the
    // first frame gets lost before the row is laid out and leaves it on day one.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 3).coerceAtLeast(0))
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (selectedIndex !in visible.drop(1).dropLast(1)) {
            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
        }
    }
    val locale = Locale.getDefault()
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days, key = { it.toEpochDay() }) { day ->
            val completion = state.completion[day] ?: 0f
            val selected = day == state.date
            val (container, content) = when {
                selected -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
                completion >= 1f -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                completion > 0f -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
            }
            Surface(
                onClick = { onSelect(day) },
                shape = RoundedCornerShape(16.dp),
                color = container,
                contentColor = content,
                border = when {
                    day == state.today -> BorderStroke(3.dp, FocusColors.Today)
                    container == MaterialTheme.colorScheme.surface ->
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    else -> null
                },
                modifier = Modifier.width(lerp(52.dp, 44.dp, collapse)).height(lerp(68.dp, 40.dp, collapse)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (collapse < 1f) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.'),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            modifier = Modifier
                                .height(lerp(16.dp, 0.dp, collapse))
                                .alpha(1f - collapse),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Collapsible summary: "n/m · %" and the perfect-days streak always visible;
 * expanded, a 4-row GitHub-style map of how complete each day was.
 */
@Composable
private fun ProgressAccordion(state: TodayUiState, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    FocusCard(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.today_summary, state.doneCount, state.dueCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.today_percent, (state.progress * 100).roundToInt()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            FaIcon(Fa.Fire, contentDescription = null, Modifier.size(18.dp), tint = FocusColors.Today)
            Spacer(Modifier.width(4.dp))
            Text(
                pluralStringResource(R.plurals.hero_streak_days, state.perfectStreak, state.perfectStreak),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            FaIcon(
                Fa.ChevronDown,
                contentDescription = stringResource(if (expanded) R.string.common_collapse else R.string.common_expand),
                Modifier.size(16.dp).rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 14.dp)) {
                Heatmap(
                    end = state.today,
                    value = { state.completion[it] },
                    today = state.today,
                    todayColor = FocusColors.Today,
                )
                Spacer(Modifier.height(10.dp))
                HeatLegend(
                    less = stringResource(R.string.heat_less),
                    more = stringResource(R.string.heat_more),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun HabitItem(
    habit: HabitRow,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    /** Off inside a category section, where the header already says it. */
    showCategory: Boolean = true,
) {
    val color = FocusColors.parse(habit.color)
    val quit = habit.kind == HabitKind.Quit
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HabitTile(habit.name, habit.icon, habit.color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (habit.done && !quit) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (habit.done && !quit) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showCategory) {
                        habit.category?.let { category ->
                            CategoryLabel(category, Modifier.weight(1f, fill = false))
                            Text(
                                " · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (habit.streak.current > 0) {
                        FaIcon(
                            if (quit) Fa.Shield else Fa.Fire,
                            contentDescription = null,
                            tint = if (quit) FocusColors.Logged else FocusColors.Today,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        habitSubtitle(habit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (quit) {
                RelapseSquare(relapsed = !habit.done, label = habit.name, onToggle = onToggle)
            } else {
                CheckSquare(checked = habit.done, color = color, label = habit.name, onToggle = onToggle)
            }
        }
    }
}

/** Streak (or days clean), period progress and goal, whatever applies. */
@Composable
private fun habitSubtitle(habit: HabitRow): String {
    val parts = mutableListOf<String>()
    habit.time?.let { parts += it.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) }
    habit.period?.let { parts += periodText(it) }
    parts += streakText(habit.streak, habit.kind)
    habit.goal?.let { goal ->
        parts += if (habit.goalProgress >= goal) {
            stringResource(R.string.goal_reached)
        } else {
            stringResource(R.string.goal_progress, habit.goalProgress, goal)
        }
    }
    return parts.joinToString(" · ")
}

@Composable
fun streakText(streak: Streak, kind: HabitKind): String = when {
    kind == HabitKind.Quit -> pluralStringResource(R.plurals.quit_clean_days, streak.current, streak.current)
    streak.current == 0 -> stringResource(R.string.habit_no_streak)
    else -> when (streak.unit) {
        StreakUnit.Days -> pluralStringResource(R.plurals.habit_streak_days, streak.current, streak.current)
        StreakUnit.Times -> pluralStringResource(R.plurals.habit_streak_times, streak.current, streak.current)
        StreakUnit.Weeks -> pluralStringResource(R.plurals.habit_streak_weeks, streak.current, streak.current)
        StreakUnit.Months -> pluralStringResource(R.plurals.habit_streak_months, streak.current, streak.current)
    }
}

@Composable
private fun periodText(period: PeriodProgress): String = stringResource(
    if (period.unit == StreakUnit.Weeks) R.string.period_this_week else R.string.period_this_month,
    period.done,
    period.target,
)

/** Quit habits: clean by default; tapping logs a relapse for the day. */
@Composable
private fun RelapseSquare(relapsed: Boolean, label: String, onToggle: () -> Unit) {
    val state = stringResource(if (relapsed) R.string.quit_state_relapsed else R.string.quit_state_clean)
    val color = if (relapsed) MaterialTheme.colorScheme.error else FocusColors.Logged
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                contentDescription = label
                stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(color.copy(alpha = if (relapsed) 1f else 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            FaIcon(
                if (relapsed) Fa.Close else Fa.Shield,
                null,
                tint = if (relapsed) Color.White else color,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Big square check on the right, like the reference; its own tap target, separate from opening the habit. */
@Composable
private fun CheckSquare(checked: Boolean, color: Color, label: String, onToggle: () -> Unit) {
    val state = stringResource(if (checked) R.string.habit_state_done else R.string.habit_state_pending)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                contentDescription = label
                stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(if (checked) color else Color.Transparent, RoundedCornerShape(8.dp))
                .border(2.dp, if (checked) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) FaIcon(Fa.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** Section title of a category in Today: icon and name (or "No category") and done/total. */
@Composable
private fun CategoryHeader(category: Category?, rows: List<HabitRow>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (category != null) {
            CategoryLabel(category, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        } else {
            Text(
                stringResource(R.string.list_no_category),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            "${rows.count { it.done }}/${rows.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
