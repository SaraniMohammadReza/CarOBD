package com.sarani.carobd.ui.screens.terminal

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.domain.usecase.SendRawCommandUseCase
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.launch

data class TerminalLog(
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val response: String,
    val isSuccess: Boolean
)

data class TerminalState(
    val logs: List<TerminalLog> = emptyList(),
    val inputCommand: String = "",
    val isSending: Boolean = false
) : UiState

sealed interface TerminalIntent : UiIntent {
    data class UpdateInput(val text: String) : TerminalIntent
    data class SendCommand(val command: String) : TerminalIntent
    data object ClearLogs : TerminalIntent
}

sealed interface TerminalEffect : UiEffect

class TerminalViewModel(
    private val sendRawCommandUseCase: SendRawCommandUseCase
) : MviViewModel<TerminalState, TerminalIntent, TerminalEffect>(TerminalState()) {

    override fun onIntent(intent: TerminalIntent) {
        when (intent) {
            is TerminalIntent.UpdateInput -> setState { copy(inputCommand = intent.text) }
            is TerminalIntent.ClearLogs -> setState { copy(logs = emptyList()) }
            is TerminalIntent.SendCommand -> {
                val cmd = intent.command.trim()
                if (cmd.isBlank()) return
                viewModelScope.launch {
                    setState { copy(isSending = true) }
                    val result = sendRawCommandUseCase(cmd)
                    result.fold(
                        onSuccess = { res ->
                            val log = TerminalLog(command = cmd, response = res, isSuccess = true)
                            setState { copy(isSending = false, inputCommand = "", logs = logs + log) }
                        },
                        onFailure = { err ->
                            val log = TerminalLog(command = cmd, response = err.message ?: "ERROR", isSuccess = false)
                            setState { copy(isSending = false, inputCommand = "", logs = logs + log) }
                        }
                    )
                }
            }
        }
    }
}
