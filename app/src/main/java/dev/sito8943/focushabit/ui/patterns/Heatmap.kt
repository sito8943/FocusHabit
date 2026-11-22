package dev.sito8943.focushabit.ui.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** GitHub-like intensity: 0 = nothing, 4 = everything due was done. */
fun heatLevel(value: Float?): Int = when {
    value == null || value <= 0f -> 0
    value < 0.34f -> 1
    value < 0.67f -> 2
    value < 1f -> 3
    else -> 4
}

/**
 * Contribution-style grid with [rows] rows. Days run top to bottom, then to the
 * next column, ending at [end] in the bottom-right cell; as many columns as fit
 * the width. Month names go over the column where a month starts.
 */
@Composable
fun Heatmap(
    end: LocalDate,
    value: (LocalDate) -> Float?,
    modifier: Modifier = Modifier,
    rows: Int = 4,
    cell: Dp = 14.dp,
    gap: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    empty: Color = MaterialTheme.colorScheme.outlineVariant,
    today: LocalDate? = null,
    todayColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val locale = Locale.getDefault()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gap) / (cell + gap)).toInt().coerceAtLeast(1)
        val first = end.minusDays((columns * rows - 1).toLong())
        fun dayAt(column: Int, row: Int): LocalDate = first.plusDays((column * rows + row).toLong())

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            // Month labels: on the column holding the 1st, if there is room since the last one.
            Box(Modifier.fillMaxWidth().height(16.dp)) {
                var lastLabel = -10
                for (column in 0 until columns) {
                    val days = (0 until rows).map { dayAt(column, it) }
                    val starts = days.firstOrNull { it.dayOfMonth == 1 } ?: (if (column == 0) days.first() else null)
                    if (starts != null && column - lastLabel >= 3) {
                        lastLabel = column
                        Text(
                            starts.month.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').replaceFirstChar { it.titlecase(locale) },
                            modifier = Modifier.padding(start = (cell + gap) * column),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (column in 0 until columns) {
                        val day = dayAt(column, row)
                        HeatCell(heatLevel(value(day)), color, empty, cell, if (day == today) todayColor else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatCell(level: Int, color: Color, empty: Color, size: Dp, ring: Color?) {
    val fill = if (level == 0) empty else color.copy(alpha = LEVEL_ALPHA[level])
    Box(
        Modifier
            .size(size)
            .background(fill, RoundedCornerShape(3.dp))
            .then(if (ring != null) Modifier.border(2.dp, ring, RoundedCornerShape(3.dp)) else Modifier),
    )
}

/** "Less ▢▢▢▢▢ More". */
@Composable
fun HeatLegend(
    less: String,
    more: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    empty: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(less, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(3.dp))
        for (level in 0..4) HeatCell(level, color, empty, 11.dp, null)
        Spacer(Modifier.width(3.dp))
        Text(more, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val LEVEL_ALPHA = floatArrayOf(0f, 0.35f, 0.55f, 0.78f, 1f)

