package dev.sito8943.focushabit.feature.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.core.icons.FaGlyph
import dev.sito8943.focushabit.core.icons.FaIcons
import dev.sito8943.focushabit.core.model.HabitColors
import dev.sito8943.focushabit.core.model.HabitKind
import dev.sito8943.focushabit.ui.patterns.CompactTextField
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.FormHeight
import dev.sito8943.focushabit.ui.patterns.HabitTile
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.theme.Alice
import dev.sito8943.focushabit.ui.theme.FocusColors
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitFormScreen(
    state: HabitFormState,
    isEdit: Boolean,
    onChange: ((HabitFormState) -> HabitFormState) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { FaIcon(Fa.Close, stringResource(R.string.common_close)) }
            Text(
                stringResource(if (isEdit) R.string.form_title_edit else R.string.form_title_new),
                modifier = Modifier.weight(1f),
                fontFamily = Alice,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.height(FormHeight),
                shape = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.form_save)) }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Name, with a live preview of the tile.
            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.form_name))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HabitTile(state.name.ifBlank { "?" }, state.icon, state.color, size = FormHeight)
                    Spacer(Modifier.width(10.dp))
                    CompactTextField(
                        value = state.name,
                        onValueChange = { value -> onChange { it.copy(name = value) } },
                        placeholder = stringResource(R.string.form_name_placeholder),
                        onDone = {},
                        modifier = Modifier.weight(1f).height(FormHeight),
                    )
                }
            }

            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.form_kind))
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    HabitKind.entries.forEachIndexed { index, kind ->
                        SegmentedButton(
                            selected = state.kind == kind,
                            onClick = { onChange { it.copy(kind = kind) } },
                            shape = SegmentedButtonDefaults.itemShape(index, HabitKind.entries.size),
                        ) {
                            Text(stringResource(if (kind == HabitKind.Build) R.string.kind_build else R.string.kind_quit))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(if (state.kind == HabitKind.Build) R.string.kind_build_hint else R.string.kind_quit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconPicker(state.icon, state.color) { icon -> onChange { it.copy(icon = icon) } }

            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.form_color))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    HabitColors.palette.forEach { hex ->
                        ColorDot(hex, selected = hex == state.color) { onChange { it.copy(color = hex) } }
                    }
                }
            }

            AnimatedVisibility(state.kind == HabitKind.Build) {
                FrequencyCard(state, onChange)
            }

            FocusCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel(stringResource(R.string.form_goal))
                        Text(
                            stringResource(R.string.form_goal_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(state.goalEnabled, onCheckedChange = { on -> onChange { it.copy(goalEnabled = on) } })
                }
                AnimatedVisibility(state.goalEnabled) {
                    Stepper(
                        value = state.goal,
                        range = 1..3650,
                        label = stringResource(
                            if (state.kind == HabitKind.Build) R.string.goal_build_label else R.string.goal_quit_label,
                        ),
                        onValue = { v -> onChange { it.copy(goal = v) } },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequencyCard(state: HabitFormState, onChange: ((HabitFormState) -> HabitFormState) -> Unit) {
    val locale = Locale.getDefault()
    FocusCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.form_frequency))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FrequencyType.entries.forEach { type ->
                FilterChip(
                    selected = state.frequencyType == type,
                    onClick = { onChange { it.copy(frequencyType = type) } },
                    label = {
                        Text(
                            stringResource(
                                when (type) {
                                    FrequencyType.Daily -> R.string.freq_daily
                                    FrequencyType.Weekdays -> R.string.freq_weekdays
                                    FrequencyType.EveryNDays -> R.string.freq_every_n_days
                                    FrequencyType.PerWeek -> R.string.freq_per_week
                                    FrequencyType.PerMonth -> R.string.freq_per_month
                                },
                            ),
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when (state.frequencyType) {
            FrequencyType.Daily -> Unit
            FrequencyType.Weekdays -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val on = day in state.weekdays
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(if (on) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(role = Role.Checkbox) {
                                onChange { it.copy(weekdays = if (on) it.weekdays - day else it.weekdays + day) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            day.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                            color = if (on) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            FrequencyType.EveryNDays -> Stepper(
                state.everyDays, 2..365, stringResource(R.string.freq_every_n_days_label),
                onValue = { v -> onChange { it.copy(everyDays = v) } },
            )
            FrequencyType.PerWeek -> Stepper(
                state.perWeek, 1..7, stringResource(R.string.freq_per_week_label),
                onValue = { v -> onChange { it.copy(perWeek = v) } },
            )
            FrequencyType.PerMonth -> Stepper(
                state.perMonth, 1..31, stringResource(R.string.freq_per_month_label),
                onValue = { v -> onChange { it.copy(perMonth = v) } },
            )
        }
    }
}

/** Suggested icons, a search over the ~1400 of Font Awesome, and "none" (the initial). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: String?, color: String, onSelect: (String?) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { FaIcons.search(query) }
    val tint = FocusColors.parse(color)
    FocusCard(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.form_icon))
        Spacer(Modifier.height(10.dp))
        CompactTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.form_icon_search),
            onDone = {},
            imeAction = ImeAction.Search,
            modifier = Modifier.fillMaxWidth().height(FormHeight),
        )
        Spacer(Modifier.height(10.dp))
        if (results.isEmpty()) {
            Text(
                stringResource(R.string.form_icon_none_found),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val noIcon = stringResource(R.string.form_icon_initial)
            IconCell(selected == null, tint, noIcon, onClick = { onSelect(null) }) {
                Text("Aa", color = if (selected == null) Color.White else tint, fontWeight = FontWeight.Bold)
            }
            results.forEach { icon ->
                val isSelected = icon.name == selected
                IconCell(isSelected, tint, icon.name, onClick = { onSelect(icon.name) }) {
                    FaGlyph(icon, if (isSelected) Color.White else tint, size = 20.dp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.form_icon_credit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IconCell(
    selected: Boolean,
    tint: Color,
    description: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) tint else tint.copy(alpha = 0.12f))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.habit_select_color, hex)
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (selected) 20.dp else 16.dp)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                .padding(if (selected) 3.dp else 0.dp)
                .background(FocusColors.parse(hex), CircleShape),
        )
    }
}

/** "[–] 3 [+] times per week". */
@Composable
private fun Stepper(
    value: Int,
    range: IntRange,
    label: String,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValue((value - 1).coerceIn(range)) }, enabled = value > range.first) {
            FaIcon(Fa.Minus, "−")
        }
        Text(
            value.toString(),
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onValue((value + 1).coerceIn(range)) }, enabled = value < range.last) {
            FaIcon(Fa.Plus, "+")
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
