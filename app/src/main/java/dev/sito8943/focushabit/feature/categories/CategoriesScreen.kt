package dev.sito8943.focushabit.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.core.model.Category
import dev.sito8943.focushabit.core.model.CategoryPrefab
import dev.sito8943.focushabit.core.model.HabitColors
import dev.sito8943.focushabit.feature.form.ColorDot
import dev.sito8943.focushabit.feature.form.IconPicker
import dev.sito8943.focushabit.ui.patterns.CompactTextField
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.FormHeight
import dev.sito8943.focushabit.ui.patterns.HabitTile
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.patterns.TitleText
import dev.sito8943.focushabit.ui.theme.FocusColors
import java.util.Locale

/** The user's categories (create, edit, delete) plus ready-made ones to add. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onSave: (Category) -> Unit,
    onDelete: (Long) -> Unit,
    onAddPrefab: (CategoryPrefab, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spanish = Locale.getDefault().language == "es"
    /** Category being edited; id 0 = new. */
    var editing by rememberSaveable(stateSaver = CategorySaver) { mutableStateOf<Category?>(null) }
    var deleting by rememberSaveable(stateSaver = CategorySaver) { mutableStateOf<Category?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { FaIcon(Fa.Back, stringResource(R.string.common_back), Modifier.size(20.dp)) }
            TitleText(stringResource(R.string.categories_title), modifier = Modifier.weight(1f))
            IconButton(onClick = { editing = Category(0, "", HabitColors.default) }) {
                FaIcon(Fa.Plus, stringResource(R.string.categories_new), Modifier.size(20.dp))
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.items.isEmpty() && !state.loading) {
                Text(
                    stringResource(R.string.categories_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            state.items.forEach { item ->
                CategoryRow(item, onEdit = { editing = item.category }, onDelete = { deleting = item.category })
            }

            val suggestions = state.suggestions(spanish)
            if (suggestions.isNotEmpty() && !state.loading) {
                FocusCard(Modifier.fillMaxWidth()) {
                    SectionLabel(stringResource(R.string.categories_suggested))
                    Text(
                        stringResource(R.string.categories_suggested_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { prefab ->
                            AssistChip(
                                onClick = { onAddPrefab(prefab, spanish) },
                                label = { Text(prefab.name(spanish)) },
                                leadingIcon = {
                                    FaIcon(prefab.icon, null, Modifier.size(14.dp), tint = FocusColors.parse(prefab.color))
                                },
                                trailingIcon = { FaIcon(Fa.Plus, null, Modifier.size(12.dp)) },
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { category ->
        CategoryEditor(
            initial = category,
            onSave = {
                onSave(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.categories_delete_title, category.name)) },
            text = { Text(stringResource(R.string.categories_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(category.id)
                    deleting = null
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun CategoryRow(item: CategoryItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val category = item.category
    Surface(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HabitTile(category.name, category.icon, category.color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.categories_habit_count, item.habitCount, item.habitCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                FaIcon(
                    Fa.Delete,
                    stringResource(R.string.categories_delete_title, category.name),
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Name, color and icon of a new or existing category. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditor(initial: Category, onSave: (Category) -> Unit, onDismiss: () -> Unit) {
    var draft by rememberSaveable(initial.id, stateSaver = CategorySaver) { mutableStateOf<Category?>(initial) }
    val current = draft ?: initial
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleText(
                stringResource(if (initial.id == 0L) R.string.categories_new else R.string.categories_edit),
                fontSize = 26.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                HabitTile(current.name.ifBlank { "?" }, current.icon, current.color, size = FormHeight)
                Spacer(Modifier.width(10.dp))
                CompactTextField(
                    value = current.name,
                    onValueChange = { draft = current.copy(name = it) },
                    placeholder = stringResource(R.string.categories_name_placeholder),
                    onDone = {},
                    modifier = Modifier.weight(1f).height(FormHeight),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitColors.palette.forEach { hex ->
                    ColorDot(hex, selected = hex == current.color) { draft = current.copy(color = hex) }
                }
            }
            IconPicker(current.icon, current.color) { draft = current.copy(icon = it) }
            Button(
                onClick = { onSave(current) },
                enabled = current.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.form_save)) }
        }
    }
}

/** Saves an optional [Category] across rotation. */
private val CategorySaver = androidx.compose.runtime.saveable.Saver<Category?, List<Any?>>(
    save = { it?.let { c -> listOf(c.id, c.name, c.color, c.icon) } ?: emptyList() },
    restore = { if (it.isEmpty()) null else Category(it[0] as Long, it[1] as String, it[2] as String, it[3] as String?) },
)
