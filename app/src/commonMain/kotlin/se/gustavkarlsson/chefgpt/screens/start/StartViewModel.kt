package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.gustavkarlsson.chefgpt.SessionRepository

class StartViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private data class State(
        val loggedIn: Boolean = false,
        val inputUsername: String = "",
        val inputPassword: String = "",
    )

    sealed interface ViewState {
        data class LoggedOut(
            val username: String,
            val password: String,
            val onUsernameChange: (String) -> Unit,
            val onPasswordChange: (String) -> Unit,
            val onClickLogin: (() -> Unit)?,
        ) : ViewState

        data class LoggedIn(
            val onClickStartChatting: () -> Unit,
        ) : ViewState
    }

    private val innerState = MutableStateFlow(State())

    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    init {
        val sessionId = sessionRepository.load()
        if (sessionId != null) {
            innerState.value = State(loggedIn = true)
        }
    }

    private fun State.toViewState(): ViewState =
        if (loggedIn) {
            ViewState.LoggedIn(
                onClickStartChatting = {
                    // TODO Navigate to chat
                },
            )
        } else {
            ViewState.LoggedOut(
                username = inputUsername,
                password = inputPassword,
                onUsernameChange = { innerState.value = innerState.value.copy(inputUsername = it) },
                onPasswordChange = { innerState.value = innerState.value.copy(inputPassword = it) },
                onClickLogin =
                    if (inputUsername.isNotBlank() && inputPassword.isNotBlank()) {
                        {
                            // TODO Implement login
                        }
                    } else {
                        null
                    },
            )
        }
}
