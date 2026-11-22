package dev.sito8943.focushabit.ui.patterns

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Rounded white card on the tinted background, the base block of every screen. */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

/** Small uppercase letter-spaced label ("DAY STREAK"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.6.sp,
        color = color,
    )
}

/** Day progress ring; animates when a habit is checked. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable () -> Unit,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            val style = Stroke(width = width, cap = StrokeCap.Round)
            val inset = width / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - width, this.size.height - width)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
            if (animated > 0f) drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = style)
        }
        content()
    }
}

/**
 * GitHub-style grid: one column per week (Monday on top), ending with the week
 * of [end]. [value] is 0..1; days after [end] are not drawn.
 */
@Composable
fun DotGrid(
    end: LocalDate,
    weeks: Int,
    value: (LocalDate) -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    empty: Color = MaterialTheme.colorScheme.outlineVariant,
    dot: Dp = 10.dp,
    gap: Dp = 4.dp,
    highlight: LocalDate? = null,
    highlightColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val lastMonday = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val firstMonday = lastMonday.minusWeeks((weeks - 1).toLong())
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(weeks) { week ->
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(7) { dayIndex ->
                    val day = firstMonday.plusWeeks(week.toLong()).plusDays(dayIndex.toLong())
                    val v = value(day)
                    val fill = when {
                        day > end -> Color.Transparent
                        day == highlight && v == 0f -> highlightColor.copy(alpha = 0.55f)
                        v <= 0f -> empty
                        else -> color.copy(alpha = 0.35f + 0.65f * v)
                    }
                    Box(Modifier.size(dot).background(fill, CircleShape))
                }
            }
        }
    }
}
