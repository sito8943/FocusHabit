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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import dev.sito8943.focushabit.ui.patterns.DotGrid
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.theme.Alice
import dev.sito8943.focushabit.ui.theme.FocusColors
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitDetailScreen(
    state: HabitDetailUiState,
    onBack: () -> Unit,
    onToggleToday: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = state.habit ?: return
    val color = FocusColors.parse(habit.color)
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
            }
            Text(
                habit.name,
                modifier = Modifier.weight(1f),
                fontFamily = Alice,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    Icons.Outlined.DeleteOutline,
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
                        pluralStringResource(R.plurals.detail_days_completed, state.loggedDays.size, state.loggedDays.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(14.dp))
                DotGrid(
                    end = state.today,
                    weeks = 26,
                    value = { if (it in state.loggedDays) 1f else 0f },
                    color = color,
                    dot = 9.dp,
                    gap = 3.dp,
                    highlight = state.today,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = stringResource(R.string.detail_streak),
                    value = state.streak,
                    caption = stringResource(R.string.detail_current_days),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.detail_best),
                    value = state.bestStreak,
                    caption = stringResource(R.string.detail_all_time),
                    modifier = Modifier.weight(1f),
                )
            }

            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.detail_weekly))
                Spacer(Modifier.height(16.dp))
                WeekdayBars(state.weekdays, color, state.today.dayOfWeek)
            }
        }

        Button(
            onClick = onToggleToday,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = if (state.doneToday) {
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
            if (!state.doneToday) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                stringResource(if (state.doneToday) R.string.detail_unmark_today else R.string.detail_mark_today),
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

