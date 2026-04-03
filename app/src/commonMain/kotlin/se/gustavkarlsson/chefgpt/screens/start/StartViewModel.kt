package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.UserCredentials

private val log = Logger.withTag("${StartViewModel::class.simpleName}")

class StartViewModel(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val navigator: Navigator,
) : ViewModel() {
    // TODO Split logged in and out states
    private data class State(
        val sessionCredentials: SessionCredentials? = null,
        val chats: List<Chat> = emptyList(),
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
            val chats: List<Chat>,
            val onClickNewChat: () -> Unit,
            val onClickChat: (Chat) -> Unit,
            val onClickLogout: () -> Unit,
        ) : ViewState
    }

    private var streamChatsJob: Job = Job().apply { complete() }

    // TODO Synchronize
    private fun replaceStreamChatsJob(
        reason: String,
        work: (suspend CoroutineScope.() -> Unit)?,
    ) {
        streamChatsJob.cancel(reason)
        if (work != null) {
            streamChatsJob = viewModelScope.launch(block = work)
        }
    }

    init {
        viewModelScope.launch {
            val lastCredentials = sessionRepository.getCurrentSession() ?: return@launch
            innerState.update { it.copy(sessionCredentials = lastCredentials) }
            replaceStreamChatsJob("Loaded last session") {
                streamChats(lastCredentials)
            }
        }
    }

    private val innerState = MutableStateFlow(State())

    // TODO Introduce a loading state before we know if we're logged in or not
    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    private suspend fun streamChats(credentials: SessionCredentials) {
        chatRepository
            .getAll(credentials.sessionId)
            .collect { chatsResult ->
                chatsResult
                    .onOk { chats ->
                        innerState.update {
                            it.copy(
                                sessionCredentials = credentials,
                                chats = chats,
                            )
                        }
                    }.onErr {
                        // TODO Show error?
                    }
            }
    }

    private fun State.toViewState(): ViewState =
        if (sessionCredentials != null) {
            ViewState.LoggedIn(
                username = sessionCredentials.username.value,
                chats = chats,
                onClickNewChat = {
                    viewModelScope.launch {
                        chatRepository
                            .create(sessionCredentials.sessionId)
                            .onOk { chat ->
                                log.i { "Chat created: ${chat.id}" }
                                replaceStreamChatsJob("Created new chat") {
                                    streamChats(sessionCredentials)
                                }
                                navigator.push(Route.Chat(sessionCredentials.sessionId, chat.id))
                            }.onErr { errorResponse ->
                                // TODO Show message?
                                log.i { "Failed to create chat: ${errorResponse.errorBody}" }
                            }
                    }
                },
                onClickChat = { chat ->
                    navigator.push(Route.Chat(sessionCredentials.sessionId, chat.id))
                },
                onClickLogout = {
                    log.i { "Logging out '${sessionCredentials.username}'" }
                    replaceStreamChatsJob("Logging out", work = null)
                    viewModelScope.launch {
                        sessionRepository.logOut()
                    }
                    innerState.update {
                        it.copy(
                            sessionCredentials = null,
                            chats = emptyList(),
                            inputUsername = "",
                            inputPassword = "",
                        )
                    }
                },
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
                                sessionRepository
                                    .register(UserCredentials(inputUsername, inputPassword))
                                    .onOk { credentials ->
                                        log.i { "Registered as '$inputUsername'" }
                                        replaceStreamChatsJob("Registered new user") {
                                            streamChats(credentials)
                                        }
                                        innerState.update {
                                            it.copy(sessionCredentials = credentials)
                                        }
                                    }.onErr { errorResponse ->
                                        // TODO Show message?
                                        //  Modify state?
                                        log.i {
                                            "Registration failed for '$inputUsername': ${errorResponse.errorBody}"
                                        }
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
                                sessionRepository
                                    .login(UserCredentials(inputUsername, inputPassword))
                                    .onOk { credentials ->
                                        log.i { "Logged in as '$inputUsername'" }
                                        replaceStreamChatsJob("Logged in user") {
                                            streamChats(credentials)
                                        }
                                        innerState.update {
                                            it.copy(sessionCredentials = credentials)
                                        }
                                    }.onErr { errorResponse ->
                                        // TODO Show message?
                                        //  Modify state?
                                        log.i { "Login failed for '$inputUsername': ${errorResponse.errorBody}" }
                                    }
                            }
                        }
                    } else {
                        null
                    },
            )
        }
}
