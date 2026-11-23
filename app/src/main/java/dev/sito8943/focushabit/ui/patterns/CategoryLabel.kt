package dev.sito8943.focushabit.ui.patterns

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.ui.theme.FocusColors

/** Name to show: a suggestion's name follows the app language until it is renamed. */
@Composable
fun Category.label(): String = displayName(LocalConfiguration.current.locales[0].language == "es")

/** Category icon (tinted with its color) and name, on one line. */
@Composable
fun CategoryLabel(
    category: Category,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FaIcon(category.icon ?: Fa.Tag, null, Modifier.size(12.dp), tint = FocusColors.parse(category.color))
        Spacer(Modifier.width(4.dp))
        Text(
            category.label(),
            style = style,
            color = FocusColors.parse(category.color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
