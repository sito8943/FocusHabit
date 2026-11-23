package dev.sito8943.focushabit.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.patterns.TitleText

/** Opens the sort/filter sheet; a badge shows how many filters are on. */
@Composable
fun ListOptionsButton(options: HabitListOptions, onClick: () -> Unit) {
    val label = stringResource(R.string.list_options)
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (options.activeFilters > 0) Badge { Text("${options.activeFilters}") } }) {
            FaIcon(
                Fa.Filter,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = if (options.isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitListOptionsSheet(
    state: TodayUiState,
    onChange: (HabitListOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = state.options
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TitleText(stringResource(R.string.list_options), fontSize = 26.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { onChange(HabitListOptions()) }, enabled = !options.isDefault) {
                    Text(stringResource(R.string.list_reset))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.list_sort))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitSort.entries.forEach { sort ->
                        FilterChip(
                            selected = options.sort == sort,
                            onClick = { onChange(options.copy(sort = sort)) },
                            label = { Text(stringResource(sort.label)) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.list_status))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    StatusFilter.entries.forEachIndexed { index, status ->
                        SegmentedButton(
                            selected = options.status == status,
                            onClick = { onChange(options.copy(status = status)) },
                            shape = SegmentedButtonDefaults.itemShape(index, StatusFilter.entries.size),
                        ) { Text(stringResource(status.label)) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.form_frequency))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FrequencyKind.entries.forEach { kind ->
                        val selected = kind in options.frequencies
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onChange(options.copy(frequencies = if (selected) options.frequencies - kind else options.frequencies + kind))
                            },
                            label = { Text(stringResource(kind.label)) },
                        )
                    }
                }
            }

            if (state.categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.form_category))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val choices: List<String?> = state.categories + if (state.hasUncategorized) listOf(null) else emptyList()
                        choices.forEach { category ->
                            val selected = category in options.categories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onChange(options.copy(categories = if (selected) options.categories - category else options.categories + category))
                                },
                                label = { Text(category ?: stringResource(R.string.list_no_category)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val HabitSort.label: Int
    get() = when (this) {
        HabitSort.Default -> R.string.list_sort_default
        HabitSort.Name -> R.string.list_sort_name
        HabitSort.Status -> R.string.list_sort_status
        HabitSort.Frequency -> R.string.form_frequency
        HabitSort.Category -> R.string.form_category
    }

private val StatusFilter.label: Int
    get() = when (this) {
        StatusFilter.All -> R.string.list_status_all
        StatusFilter.Pending -> R.string.list_status_pending
        StatusFilter.Done -> R.string.list_status_done
    }

private val FrequencyKind.label: Int
    get() = when (this) {
        FrequencyKind.Daily -> R.string.freq_daily
        FrequencyKind.Weekdays -> R.string.freq_weekdays
        FrequencyKind.EveryNDays -> R.string.freq_every_n_days
        FrequencyKind.PerWeek -> R.string.freq_per_week
        FrequencyKind.PerMonth -> R.string.freq_per_month
    }
