package dev.sito8943.focushabit.feature.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sito8943.focushabit.core.model.SyncCode
import dev.sito8943.focushabit.core.network.NetworkException
import dev.sito8943.focushabit.core.network.NotConfiguredException
import dev.sito8943.focushabit.core.settings.SyncCodeStore
import dev.sito8943.focushabit.core.sync.SyncCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RecoveryMessage {
    data object Invalid : RecoveryMessage
    data object Applied : RecoveryMessage
    data object Offline : RecoveryMessage
    data object NotConfigured : RecoveryMessage
    data class Failed(val reason: String) : RecoveryMessage
}

data class RecoveryUiState(
    val restoring: Boolean = false,
    val input: String = "",
    val applying: Boolean = false,
    val message: RecoveryMessage? = null,
)

class RecoveryViewModel(
    private val codeStore: SyncCodeStore,
    private val sync: SyncCoordinator,
) : ViewModel() {

    val code: StateFlow<String> = codeStore.code

    private val _state = MutableStateFlow(RecoveryUiState())
    val state: StateFlow<RecoveryUiState> = _state.asStateFlow()

    fun showRestore(restoring: Boolean) {
        _state.update { it.copy(restoring = restoring, message = null) }
    }

    fun onInput(value: String) {
        _state.update { it.copy(input = SyncCode.normalize(value), message = null) }
    }

    /** Clears the messages and mode when the sheet closes, like the web modal. */
    fun reset() {
        if (!_state.value.applying) _state.value = RecoveryUiState()
    }

    /** Loads the code's data first and switches to it only if that worked. */
    fun apply() {
        val input = _state.value.input
        if (!SyncCode.isValid(input)) {
            _state.update { it.copy(message = RecoveryMessage.Invalid) }
            return
        }
        _state.update { it.copy(applying = true, message = null) }
        viewModelScope.launch {
            val message = try {
                sync.restore(input)
                codeStore.set(input)
                RecoveryMessage.Applied
            } catch (error: CancellationException) {
                throw error
            } catch (_: NetworkException) {
                RecoveryMessage.Offline
            } catch (_: NotConfiguredException) {
                RecoveryMessage.NotConfigured
            } catch (error: Exception) {
                RecoveryMessage.Failed(error.message.orEmpty())
            }
            _state.update {
                if (message == RecoveryMessage.Applied) {
                    RecoveryUiState(message = message)
                } else {
                    it.copy(applying = false, message = message)
                }
            }
        }
    }
}
