package dev.sito8943.focushabit.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.BuildConfig
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.core.settings.AppLanguage
import dev.sito8943.focushabit.core.settings.ThemeMode
import dev.sito8943.focushabit.core.settings.WeekStart
import dev.sito8943.focushabit.ui.patterns.FocusCard
import dev.sito8943.focushabit.ui.patterns.SectionLabel
import dev.sito8943.focushabit.ui.patterns.TitleText

@Composable
fun SettingsScreen(
    language: AppLanguage,
    theme: ThemeMode,
    weekStart: WeekStart,
    onLanguage: (AppLanguage) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onWeekStart: (WeekStart) -> Unit,
    onRecoveryCode: () -> Unit,
    onCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { FaIcon(Fa.Back, stringResource(R.string.common_back), Modifier.size(20.dp)) }
            TitleText(
                stringResource(R.string.settings_title),
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Choice(
                label = stringResource(R.string.settings_language),
                options = AppLanguage.entries,
                selected = language,
                text = {
                    when (it) {
                        AppLanguage.System -> stringResource(R.string.settings_system)
                        AppLanguage.Spanish -> "Español"
                        AppLanguage.English -> "English"
                    }
                },
                onSelect = onLanguage,
            )
            Choice(
                label = stringResource(R.string.settings_theme),
                options = ThemeMode.entries,
                selected = theme,
                text = {
                    stringResource(
                        when (it) {
                            ThemeMode.System -> R.string.settings_system
                            ThemeMode.Light -> R.string.settings_theme_light
                            ThemeMode.Dark -> R.string.settings_theme_dark
                        },
                    )
                },
                onSelect = onTheme,
            )
            Choice(
                label = stringResource(R.string.settings_week_start),
                options = WeekStart.entries,
                selected = weekStart,
                text = {
                    stringResource(if (it == WeekStart.Sunday) R.string.settings_sunday else R.string.settings_monday)
                },
                onSelect = onWeekStart,
            )

            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_data))
                Spacer(Modifier.height(8.dp))
                SettingsLink(Fa.Tag, stringResource(R.string.categories_title), stringResource(R.string.settings_categories_hint), onCategories)
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = onRecoveryCode,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        FaIcon(Fa.Key, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.recovery_title), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.settings_recovery_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FaIcon(Fa.ChevronRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            FocusCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.settings_about))
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.form_icon_credit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.settings_fonts_credit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choice(
    label: String,
    options: List<T>,
    selected: T,
    text: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FocusCard(Modifier.fillMaxWidth()) {
        SectionLabel(label)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(text(option), maxLines = 1) }
            }
        }
    }
}

@Composable
private fun SettingsLink(icon: String, title: String, hint: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            FaIcon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FaIcon(Fa.ChevronRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
