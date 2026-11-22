package dev.sito8943.focushabit.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.theme.Alice
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.ui.theme.FocusColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.calendar_previous_month),
                )
            }
            Text(
                text = "${state.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${state.month.year}"
                    .replaceFirstChar { it.titlecase(locale) },
                modifier = Modifier.weight(1f),
                fontFamily = Alice,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.calendar_next_month),
                )
            }
        }

        FocusCard(Modifier.fillMaxWidth()) {
        // Sunday first, like the web.
        val weekStart = DayOfWeek.SUNDAY
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            (0L until 7L).forEach { offset ->
                SectionLabel(
                    text = weekStart.plus(offset).getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally),
                )
            }
        }

        val first = state.month.atDay(1)
        val leading = (first.dayOfWeek.value % 7) // Sunday = 0
        val cells: List<LocalDate?> = List(leading) { null } +
            (1..state.month.lengthOfMonth()).map { state.month.atDay(it) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cells.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0 until 7).forEach { index ->
                        val day = week.getOrNull(index)
                        Box(Modifier.weight(1f).aspectRatio(1f)) {
                            if (day != null) {
                                DayCell(day, state.appearance(day)) { onSelect(day) }
                            }
                        }
                    }
                }
            }
        }
        }

        Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(MaterialTheme.colorScheme.tertiary, stringResource(R.string.calendar_legend_selected))
            LegendItem(FocusColors.Logged, stringResource(R.string.calendar_legend_logged))
            LegendItem(FocusColors.Today, stringResource(R.string.calendar_legend_today))
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, appearance: DayAppearance, onClick: () -> Unit) {
    val (container, content, border) = when (appearance) {
        DayAppearance.Selected -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            null,
        )
        DayAppearance.Logged -> Triple(FocusColors.Logged, FocusColors.OnStatus, null)
        DayAppearance.Today -> Triple(Color.Transparent, FocusColors.Today, FocusColors.Today)
        DayAppearance.Normal -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.outlineVariant,
        )
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        border = border?.let { BorderStroke(if (appearance == DayAppearance.Today) 2.dp else 1.dp, it) },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (appearance == DayAppearance.Normal) FontWeight.Normal else FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(Modifier.size(12.dp), shape = CircleShape, color = color) {}
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
