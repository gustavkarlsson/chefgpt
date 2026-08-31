package se.gustavkarlsson.chefgpt.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessage
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessages

private const val SUBSCRIPTION_TIMEOUT_MS = 5000L

abstract class StateViewModel<State : Any, UiState : Any> : ViewModel() {
    protected abstract fun createInitialState(): State

    protected abstract fun State.toUiState(): UiState

    protected val innerState: MutableStateFlow<State> by lazy {
        MutableStateFlow(createInitialState())
    }

    val uiState: StateFlow<UiState> by lazy {
        innerState
            .map { it.toUiState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = innerState.value.toUiState(),
            )
    }

    private val snackbar = SnackbarMessages()
    val snackbarMessages: Flow<SnackbarMessage> = snackbar.messages

    protected fun showSnackbar(message: SnackbarMessage) = snackbar.show(message)

    protected fun showSnackbar(
        text: String,
        isError: Boolean = false,
    ) = snackbar.show(text = text, isError = isError)
}
