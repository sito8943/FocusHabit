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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.ui.theme.FocusColors

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
            category.name,
            style = style,
            color = FocusColors.parse(category.color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
