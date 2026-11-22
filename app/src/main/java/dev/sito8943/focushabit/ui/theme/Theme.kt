package dev.sito8943.focushabit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Tokens from the web's `src/index.css`. */
object FocusColors {
    val Primary = Color(0xFF00997B)
    val PrimaryStrong = Color(0xFF006654)
    val PrimaryHover = Color(0xFF1AB89A)

    /** Calendar: selected day. */
    val Info = Color(0xFF0070BA)

    /** Calendar: a day with any habit done. */
    val Success = Color(0xFF1DA05F)

    /** Calendar: today, when not selected nor done. */
    val Warning = Color(0xFFFFA500)
    val OnWarning = Color(0xFF000000)
    val Error = Color(0xFFC34444)
    val OnStatus = Color(0xFFFBFBFB)

    /** `#rrggbb` from the server, or [Primary] if it is not one. */
    fun parse(hex: String): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Primary)
}

private val Light = lightColorScheme(
    primary = FocusColors.PrimaryStrong,
    onPrimary = Color.White,
    primaryContainer = FocusColors.Primary.copy(alpha = 0.16f),
    onPrimaryContainer = FocusColors.PrimaryStrong,
    secondary = Color(0xFFD7942E),
    background = Color(0xFFF2F2F2),
    onBackground = Color(0xFF0D0D0D),
    surface = Color.White,
    onSurface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF4D4D4D),
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFF7F7F7),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
    error = FocusColors.Error,
)

private val Dark = darkColorScheme(
    primary = FocusColors.PrimaryHover,
    onPrimary = Color(0xFF00201A),
    primaryContainer = FocusColors.Primary.copy(alpha = 0.24f),
    onPrimaryContainer = FocusColors.PrimaryHover,
    secondary = Color(0xFFEBB04A),
    background = Color(0xFF121614),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF1B201E),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A302E),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainer = Color(0xFF1B201E),
    surfaceContainerLow = Color(0xFF181C1B),
    surfaceContainerHigh = Color(0xFF232927),
    outline = Color(0xFF4D5552),
    outlineVariant = Color(0xFF333A37),
    error = Color(0xFFE57373),
)

@Composable
fun FocusHabitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
