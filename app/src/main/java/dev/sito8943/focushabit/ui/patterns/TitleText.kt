package dev.sito8943.focushabit.ui.patterns

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.ui.theme.Bebas

/**
 * Bebas Neue title whose capitals are optically centred in its box, so a Row
 * with `CenterVertically` lines it up with icons.
 *
 * Bebas has capitals only (cap height 700/1000) but keeps a 300-unit descender
 * in its metrics: with the line box trimmed to the font size and centred, the
 * capitals still sit 0.05 em high (hhea 900/-300 → box centre 300 units above
 * the baseline, capitals' centre at 350). The offset moves them down by that.
 */
@Composable
fun TitleText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    val shift = with(LocalDensity.current) { (fontSize * CAP_CENTRE_OFFSET_EM).toDp() }
    Text(
        text = text,
        modifier = modifier.offset(y = shift),
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        style = TextStyle(
            fontFamily = Bebas,
            fontSize = fontSize,
            lineHeight = fontSize,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
    )
}

private const val CAP_CENTRE_OFFSET_EM = 0.05f
