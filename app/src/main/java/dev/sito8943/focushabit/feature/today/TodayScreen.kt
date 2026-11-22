package dev.sito8943.focushabit.feature.today

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.model.HabitColors
import dev.sito8943.focushabit.ui.patterns.Celebration
import dev.sito8943.focushabit.ui.patterns.DotGrid
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.ProgressRing
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.theme.Alice
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
    onAdd: (String, String) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Celebrate the moment the last habit of today gets checked, not on opening
    // a day that was already complete.
    var celebrate by remember { mutableStateOf(false) }
    var previous by remember { mutableStateOf<Pair<LocalDate, Boolean>?>(null) }
    val allDone = state.habits.isNotEmpty() && state.doneCount == state.habits.size
    LaunchedEffect(state.loading, state.date, allDone) {
        if (state.loading) return@LaunchedEffect
        val before = previous
        if (before != null && before.first == state.date && !before.second && allDone && state.isToday) {
            celebrate = true
        }
        previous = state.date to allDone
    }

    Box(modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") { DayHeader(state, onBackToToday) }
            item(key = "strip") { WeekStrip(state, onSelectDay) }
            item(key = "hero") { HeroCard(state, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
            item(key = "label") {
                SectionLabel(
                    stringResource(R.string.today_habits),
                    Modifier.padding(start = 20.dp, top = 8.dp),
                )
            }
            if (state.habits.isEmpty() && !state.loading) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.today_empty),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.habits, key = { it.id }) { habit ->
                HabitItem(
                    habit = habit,
                    onToggle = { onToggle(habit.id) },
                    onOpen = { onOpen(habit.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        HabitForm(onAdd)
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
            Text(
                text = when (state.date) {
                    state.today -> stringResource(greeting(LocalTime.now().hour))
                    state.today.minusDays(1) -> stringResource(R.string.day_yesterday)
                    state.today.plusDays(1) -> stringResource(R.string.day_tomorrow)
                    else -> state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                },
                fontFamily = Alice,
                fontSize = 34.sp,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!state.isToday) {
            TextButton(onClick = onBackToToday) { Text(stringResource(R.string.today_back_to_today)) }
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
private fun WeekStrip(state: TodayUiState, onSelect: (LocalDate) -> Unit) {
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
                    day == state.today && !selected -> BorderStroke(2.dp, FocusColors.Today)
                    container == MaterialTheme.colorScheme.surface ->
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    else -> null
                },
                modifier = Modifier.width(52.dp).height(68.dp),
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
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.'),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

/** Day progress ring and overall streak with the last weeks as dots. */
@Composable
private fun HeroCard(state: TodayUiState, modifier: Modifier = Modifier) {
    FocusCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = state.progress, size = 104.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.today_percent, (state.progress * 100).roundToInt()),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.today_summary, state.doneCount, state.habits.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.overallStreak.toString(),
                    fontSize = 48.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                SectionLabel(stringResource(R.string.hero_day_streak))
                Spacer(Modifier.height(10.dp))
                DotGrid(
                    end = state.today,
                    weeks = 8,
                    value = { state.completion[it] ?: 0f },
                    dot = 8.dp,
                    gap = 3.dp,
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
) {
    val color = FocusColors.parse(habit.color)
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(color.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    habit.name.take(1).uppercase(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (habit.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (habit.done) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (habit.streak > 0) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = FocusColors.Today,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        if (habit.streak > 0) {
                            pluralStringResource(R.plurals.habit_streak_days, habit.streak, habit.streak)
                        } else {
                            stringResource(R.string.habit_no_streak)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CheckSquare(checked = habit.done, color = color, label = habit.name, onToggle = onToggle)
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
            if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HabitForm(onAdd: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf(HabitColors.default) }
    val submit = {
        if (name.isNotBlank()) {
            onAdd(name, color)
            name = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompactTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.habit_new_placeholder),
                onDone = submit,
                modifier = Modifier.weight(1f).height(FORM_HEIGHT),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = submit,
                enabled = name.isNotBlank(),
                modifier = Modifier.height(FORM_HEIGHT),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.habit_add))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HabitColors.palette.forEach { hex ->
                ColorSwatch(hex = hex, selected = hex == color, onClick = { color = hex })
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.habit_select_color, hex)
    // 16dp dot (half the old size) inside a 32dp tap area, so it stays easy to hit.
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (selected) 20.dp else 16.dp)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                .padding(if (selected) 3.dp else 0.dp)
                .background(FocusColors.parse(hex), CircleShape),
        )
    }
}

private val FORM_HEIGHT = 44.dp

/** Single-line field at button height (OutlinedTextField cannot go below 56dp). */
@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(
                        if (focused) 2.dp else 1.dp,
                        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                field()
            }
        },
    )
}
