package dev.sito8943.focushabit.feature.recovery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.R
import dev.sito8943.focushabit.core.icons.Fa
import dev.sito8943.focushabit.core.icons.FaIcon
import dev.sito8943.focushabit.ui.theme.Alice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySheet(
    code: String,
    state: RecoveryUiState,
    onInput: (String) -> Unit,
    onApply: () -> Unit,
    onShowRestore: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.recovery_title),
                fontFamily = Alice,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                stringResource(R.string.recovery_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!state.restoring) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 48.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = code,
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = 0.25.em,
                            textAlign = TextAlign.Center,
                        )
                        IconButton(onClick = { copy(context, code) }) {
                            FaIcon(Fa.Copy, stringResource(R.string.recovery_copy))
                        }
                    }
                }
                TextButton(onClick = { onShowRestore(true) }) {
                    Text(stringResource(R.string.recovery_have_code))
                }
            } else {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInput,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.recovery_restore_label)) },
                    placeholder = { Text("AB12", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    enabled = !state.applying,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.25.em,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { onApply() }),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApply, enabled = !state.applying) {
                        Text(stringResource(R.string.recovery_use_code))
                    }
                    TextButton(onClick = { onShowRestore(false) }, enabled = !state.applying) {
                        Text(stringResource(R.string.recovery_show_mine))
                    }
                }
            }

            state.message?.let { Notice(it) }
        }
    }
}

@Composable
private fun Notice(message: RecoveryMessage) {
    val success = message == RecoveryMessage.Applied
    val text = when (message) {
        RecoveryMessage.Applied -> stringResource(R.string.recovery_applied)
        RecoveryMessage.Invalid -> stringResource(R.string.recovery_invalid)
        RecoveryMessage.Offline -> stringResource(R.string.recovery_offline)
        RecoveryMessage.NotConfigured -> stringResource(R.string.sync_not_configured)
        is RecoveryMessage.Failed -> stringResource(R.string.recovery_failed, message.reason)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        contentColor = if (success) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun copy(context: Context, code: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Focus Habit", code))
    Toast.makeText(context, R.string.recovery_copied, Toast.LENGTH_SHORT).show()
}
