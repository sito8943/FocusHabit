package dev.sito8943.focushabit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.sito8943.focushabit.R

/** Brand palette (Pantone sheet): red, navy, green, gold, black. */
object Brand {
    val Red = Color(0xFFCD212A) // PMS 18-1662 TCX
    val Navy = Color(0xFF041E42) // PMS 282 C
    val Green = Color(0xFF00997B)
    val Gold = Color(0xFFD7942E) // PMS 15-1051 TCX
    val Black = Color(0xFF1B1B1B) // PMS Black 7

    /** The sheet's 30/50/80% steps, as tints over white. */
    fun Color.tint(percent: Int): Color = copy(alpha = percent / 100f).compositeOver(Color.White)
}

/** Semantic colours used outside Material's scheme. */
object FocusColors {
    /** Calendar: selected day. */
    val Selected = Brand.Navy

    /** Calendar: a day with any habit done. */
    val Logged = Brand.Green

    /** Calendar: today, when not selected nor done. Also the streak flame. */
    val Today = Brand.Gold
    val OnStatus = Color.White

    /** `#rrggbb` from the server, or brand green if it is not one. */
    fun parse(hex: String): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Brand.Green)
}

@OptIn(ExperimentalTextApi::class)
private fun roboto(weight: FontWeight) = Font(
    R.font.roboto_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Body font of the brand (the web's "Roboto Variable"), bundled so Samsung's One UI font does not replace it. */
val Roboto = FontFamily(
    roboto(FontWeight.Normal),
    roboto(FontWeight.Medium),
    roboto(FontWeight.SemiBold),
    roboto(FontWeight.Bold),
)

/** Display font for the wordmark (the web's h1). */
val Alice = FontFamily(Font(R.font.alice_regular))

private val Light = with(Brand) {
    lightColorScheme(
        primary = Green,
        onPrimary = Color.White,
        primaryContainer = Green.tint(15),
        onPrimaryContainer = Color(0xFF00594A),
        secondary = Gold,
        onSecondary = Color.White,
        secondaryContainer = Gold.tint(20),
        onSecondaryContainer = Color(0xFF5C3B07),
        tertiary = Navy,
        onTertiary = Color.White,
        tertiaryContainer = Navy.tint(12),
        onTertiaryContainer = Navy,
        background = Color(0xFFFAFAFA),
        onBackground = Black,
        surface = Color.White,
        onSurface = Black,
        surfaceVariant = Navy.tint(8),
        onSurfaceVariant = Color(0xFF4A5468),
        surfaceContainer = Navy.tint(5),
        surfaceContainerLow = Color.White,
        surfaceContainerHigh = Navy.tint(8),
        outline = Navy.tint(30),
        outlineVariant = Navy.tint(14),
        error = Red,
        onError = Color.White,
        errorContainer = Red.tint(15),
        onErrorContainer = Color(0xFF7A0F15),
    )
}

private val Dark = with(Brand) {
    darkColorScheme(
        primary = Color(0xFF3DBFA0),
        onPrimary = Color(0xFF00201A),
        primaryContainer = Green.copy(alpha = 0.28f).compositeOver(Black),
        onPrimaryContainer = Color(0xFF8FE3CC),
        secondary = Color(0xFFEBB04A),
        onSecondary = Color(0xFF2A1A00),
        tertiary = Color(0xFF9DB4D9),
        onTertiary = Navy,
        background = Black,
        onBackground = Color(0xFFEDEDED),
        surface = Color(0xFF242424),
        onSurface = Color(0xFFEDEDED),
        surfaceVariant = Color(0xFF2E3440),
        onSurfaceVariant = Color(0xFFB4BCCB),
        surfaceContainer = Color(0xFF222222),
        surfaceContainerLow = Color(0xFF1F1F1F),
        surfaceContainerHigh = Color(0xFF2A2A2A),
        outline = Color(0xFF5A6273),
        outlineVariant = Color(0xFF3A3F49),
        error = Color(0xFFE5575E),
        onError = Color.White,
    )
}

private val Base = Typography()

private val FocusTypography = Typography(
    displayLarge = Base.displayLarge.roboto(),
    displayMedium = Base.displayMedium.roboto(),
    displaySmall = Base.displaySmall.roboto(),
    headlineLarge = Base.headlineLarge.roboto(),
    headlineMedium = Base.headlineMedium.roboto(),
    headlineSmall = Base.headlineSmall.roboto(),
    titleLarge = Base.titleLarge.roboto(),
    titleMedium = Base.titleMedium.roboto(),
    titleSmall = Base.titleSmall.roboto(),
    bodyLarge = Base.bodyLarge.roboto(),
    bodyMedium = Base.bodyMedium.roboto(),
    bodySmall = Base.bodySmall.roboto(),
    labelLarge = Base.labelLarge.roboto(),
    labelMedium = Base.labelMedium.roboto(),
    labelSmall = Base.labelSmall.roboto(),
)

private fun TextStyle.roboto() = copy(fontFamily = Roboto)

@Composable
fun FocusHabitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = FocusTypography,
        content = content,
    )
}
