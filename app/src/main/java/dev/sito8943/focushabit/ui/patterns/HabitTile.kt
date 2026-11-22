package dev.sito8943.focushabit.ui.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sito8943.focushabit.core.icons.FaGlyph
import dev.sito8943.focushabit.core.icons.FaIcons
import dev.sito8943.focushabit.ui.theme.FocusColors

/** Rounded square in the habit's colour with its icon, or its initial when it has none. */
@Composable
fun HabitTile(
    name: String,
    icon: String?,
    color: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val tint = FocusColors.parse(color)
    Box(
        modifier.size(size).background(tint.copy(alpha = 0.18f), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        val glyph = FaIcons.find(icon)
        if (glyph != null) {
            FaGlyph(glyph, tint, size = size * 0.46f)
        } else {
            Text(
                name.trim().take(1).uppercase(),
                color = tint,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
