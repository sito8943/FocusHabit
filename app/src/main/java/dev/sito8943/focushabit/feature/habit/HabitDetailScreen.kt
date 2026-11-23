package dev.sito8943.focushabit.feature.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.core.model.StreakUnit
import dev.sito8943.focushabit.ui.patterns.HabitTile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.ui.patterns.DotGrid
import dev.sito8943.focushabit.ui.patterns.CategoryLabel
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.patterns.TitleText
import dev.sito8943.focushabit.ui.theme.FocusColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitDetailScreen(
    state: HabitDetailUiState,
    onBack: () -> Unit,
    onToggleToday: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = state.habit ?: return
    val color = FocusColors.parse(habit.color)
    val quit = habit.kind == HabitKind.Quit
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                FaIcon(Fa.Back, stringResource(R.string.common_back))
            }
            HabitTile(habit.name, habit.icon, habit.color, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TitleText(
                    habit.name,
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.category?.let { CategoryLabel(it) }
            }
            IconButton(onClick = onEdit) {
                FaIcon(Fa.Edit, stringResource(R.string.habit_edit))
            }
            IconButton(onClick = { confirmDelete = true }) {
                FaIcon(
                    Fa.Delete,
                    stringResource(R.string.habit_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FocusCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(R.string.detail_momentum), Modifier.weight(1f))
                    Text(
                        pluralStringResource(
                            if (quit) R.plurals.detail_days_clean else R.plurals.detail_days_completed,
                            state.goodDays.size,
                            state.goodDays.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(14.dp))
                DotGrid(
                    end = state.today,
                    weeks = 26,
                    value = { if (it in state.goodDays) 1f else 0f },
                    color = color,
                    dot = 9.dp,
                    gap = 3.dp,
                    highlight = state.today,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            state.nextDue?.let { next ->
                FocusCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(stringResource(R.string.detail_next), Modifier.weight(1f))
                        Text(
                            nextDueText(next, state.today, habit.time),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = stringResource(if (quit) R.string.detail_clean else R.string.detail_streak),
                    value = state.streak.current,
                    caption = unitText(state.streak.unit),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.detail_best),
                    value = state.streak.best,
                    caption = stringResource(R.string.detail_all_time),
                    modifier = Modifier.weight(1f),
                )
            }

            habit.goal?.let { goal ->
                FocusCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(stringResource(R.string.form_goal), Modifier.weight(1f))
                        Text(
                            if (state.goalProgress >= goal) {
                                stringResource(R.string.goal_reached)
                            } else {
                                stringResource(R.string.goal_progress, state.goalProgress, goal)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (state.goalProgress.toFloat() / goal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.15f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }

            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(if (quit) R.string.detail_relapses_weekly else R.string.detail_weekly))
                Spacer(Modifier.height(16.dp))
                WeekdayBars(state.weekdays, if (quit) MaterialTheme.colorScheme.error else color, state.today.dayOfWeek)
            }
        }

        Button(
            onClick = onToggleToday,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = if (state.loggedToday) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            },
        ) {
            if (!state.loggedToday) {
                FaIcon(if (quit) Fa.Close else Fa.Check, null)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                stringResource(
                    when {
                        quit && state.loggedToday -> R.string.detail_undo_relapse
                        quit -> R.string.detail_relapse_today
                        state.loggedToday -> R.string.detail_unmark_today
                        else -> R.string.detail_mark_today
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.habit_delete_title, habit.name)) },
            text = { Text(stringResource(R.string.habit_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun StatCard(label: String, value: Int, caption: String, modifier: Modifier = Modifier) {
    FocusCard(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel(label)
            Text(
                value.toString(),
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Logs per weekday, last 12 weeks. Today's weekday in gold, the rest in the habit colour. */
@Composable
private fun WeekdayBars(counts: Map<DayOfWeek, Int>, color: Color, today: DayOfWeek) {
    val max = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val locale = Locale.getDefault()
    Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DayOfWeek.entries.forEach { day ->
            val count = counts[day] ?: 0
            Column(
                Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((110 * count / max).coerceAtLeast(8).dp)
                        .background(
                            when {
                                day == today -> FocusColors.Today
                                count == 0 -> MaterialTheme.colorScheme.outlineVariant
                                else -> color.copy(alpha = 0.45f + 0.55f * count / max)
                            },
                            RoundedCornerShape(50),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun unitText(unit: StreakUnit): String = stringResource(
    when (unit) {
        StreakUnit.Days -> R.string.unit_days
        StreakUnit.Times -> R.string.unit_times
        StreakUnit.Weeks -> R.string.unit_weeks
        StreakUnit.Months -> R.string.unit_months
    },
)

/** "Today", "Tomorrow" or a short date, plus the habit's time if it has one. */
@Composable
private fun nextDueText(next: LocalDate, today: LocalDate, time: LocalTime?): String {
    val day = when (next) {
        today -> stringResource(R.string.detail_next_today)
        today.plusDays(1) -> stringResource(R.string.detail_next_tomorrow)
        else -> next.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }
    return time?.let { "$day · ${it.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}" } ?: day
}
