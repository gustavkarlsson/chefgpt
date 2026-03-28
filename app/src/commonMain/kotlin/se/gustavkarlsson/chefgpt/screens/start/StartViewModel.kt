package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.Navigator
import se.gustavkarlsson.chefgpt.Route
import se.gustavkarlsson.chefgpt.SessionCredentials
import se.gustavkarlsson.chefgpt.SessionRepository
import se.gustavkarlsson.chefgpt.api.SessionId

class StartViewModel(
    private val sessionRepository: SessionRepository,
    private val client: ChefGptClient,
) : ViewModel() {
    private data class State(
        val username: String? = null,
        val sessionId: SessionId? = null,
        val inputUsername: String = "",
        val inputPassword: String = "",
    )

    sealed interface ViewState {
        data class LoggedOut(
            val username: String,
            val password: String,
            val onUsernameChange: (String) -> Unit,
            val onPasswordChange: (String) -> Unit,
            val onClickRegister: (() -> Unit)?,
            val onClickLogin: (() -> Unit)?,
        ) : ViewState

        data class LoggedIn(
            val username: String,
            val onClickStartChatting: (Navigator) -> Unit,
        ) : ViewState
    }

    private val innerState = MutableStateFlow(State())

    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    init {
        val credentials = sessionRepository.load()
        if (credentials != null) {
            innerState.value = State(username = credentials.username, sessionId = credentials.sessionId)
        }
    }

    private fun State.toViewState(): ViewState =
        if (username != null && sessionId != null) {
            ViewState.LoggedIn(
                username = username,
                onClickStartChatting = { navigator -> navigator.replace(Route.Chat(sessionId)) },
            )
        } else {
            ViewState.LoggedOut(
                username = inputUsername,
                password = inputPassword,
                onUsernameChange = { innerState.value = innerState.value.copy(inputUsername = it) },
                onPasswordChange = { innerState.value = innerState.value.copy(inputPassword = it) },
                onClickRegister =
                    if (inputUsername.isNotBlank() && inputPassword.isNotBlank()) {
                        {
                            viewModelScope.launch {
                                client
                                    .register(inputUsername, inputPassword)
                                    .onOk { sessionId ->
                                        sessionRepository.save(SessionCredentials(inputUsername, sessionId))
                                        innerState.update { it.copy(username = inputUsername, sessionId = sessionId) }
                                    }.onErr { errorResponse ->
                                        // TODO Show message?
                                        //  Modify state?
                                    }
                            }
                        }
                    } else {
                        null
                    },
                onClickLogin =
                    if (inputUsername.isNotBlank() && inputPassword.isNotBlank()) {
                        {
                            viewModelScope.launch {
                                client
                                    .login(inputUsername, inputPassword)
                                    .onOk { sessionId ->
                                        sessionRepository.save(SessionCredentials(inputUsername, sessionId))
                                        innerState.update { it.copy(username = inputUsername, sessionId = sessionId) }
                                    }.onErr { errorResponse ->
                                        // TODO Show message?
                                        //  Modify state?
                                    }
                            }
                        }
                    } else {
                        null
                    },
            )
        }
}
