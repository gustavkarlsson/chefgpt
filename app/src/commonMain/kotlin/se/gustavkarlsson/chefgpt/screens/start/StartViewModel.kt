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
import se.gustavkarlsson.chefgpt.sessions.RegisterError
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.UserCredentials

private val log = Logger.withTag("${StartViewModel::class.simpleName}")

class StartViewModel(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val navigator: Navigator,
) : ViewModel() {
    // TODO Split into loading, logging in, and logging out states
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
            val onClickDeleteChat: (Chat) -> Unit,
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
            // Ignore errors, as we can just start with a fresh session
            sessionRepository
                .getCurrentSession()
                .onOk { lastCredentials ->
                    if (lastCredentials != null) {
                        innerState.update { it.copy(sessionCredentials = lastCredentials) }
                        replaceStreamChatsJob("Loaded last session") {
                            streamChats(lastCredentials)
                        }
                    }
                }
        }
    }

    private val innerState = MutableStateFlow(State())

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
                    }.onErr { errorResponse ->
                        // TODO Show user-friendly error
                        log.e { "Failed to stream chats: $errorResponse" }
                        // TODO Recover somehow
                    }
            }
    }

    private fun State.toViewState(): ViewState =
        if (sessionCredentials == null) {
            ViewState.LoggedOut(
                username = inputUsername,
                password = inputPassword,
                onUsernameChange = { innerState.value = innerState.value.copy(inputUsername = it) },
                onPasswordChange = { innerState.value = innerState.value.copy(inputPassword = it) },
                onClickRegister =
                    if (inputUsername.isNotBlank() && inputPassword.isNotBlank()) {
                        { onClickRegister(inputUsername, inputPassword) }
                    } else {
                        null
                    },
                onClickLogin =
                    if (inputUsername.isNotBlank() && inputPassword.isNotBlank()) {
                        { onClickLogin(this.inputUsername, this.inputPassword) }
                    } else {
                        null
                    },
            )
        } else {
            ViewState.LoggedIn(
                username = sessionCredentials.username.value,
                chats = chats,
                onClickNewChat = { onClickNewChat(sessionCredentials) },
                onClickChat = { chat -> navigator.push(Route.Chat(sessionCredentials.sessionId, chat.id)) },
                onClickDeleteChat = { chat -> onClickDeleteChat(sessionCredentials, chat) },
                onClickLogout = { onClickLogout() },
            )
        }

    private fun onClickRegister(
        inputUsername: String,
        inputPassword: String,
    ) {
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
                }.onErr { error ->
                    // TODO Show correct feedback message based on the error
                    //  Modify state?
                    when (error) {
                        is RegisterError.ServerError ->
                            log.i {
                                "Registration failed for '$inputUsername': ${error.response.errorBody}"
                            }
                        RegisterError.StorageFailed ->
                            log.e { "Registration succeeded but failed to save session for '$inputUsername'" }
                    }
                }
        }
    }

    private fun onClickLogin(
        inputUsername: String,
        inputPassword: String,
    ) {
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
                    // TODO Show correct feedback message based on the status code
                    //  Modify state?
                    log.i { "Login failed for '$inputUsername': ${errorResponse.errorBody}" }
                }
        }
    }

    private fun onClickNewChat(sessionCredentials: SessionCredentials) {
        viewModelScope.launch {
            chatRepository
                .create(sessionCredentials.sessionId)
                .onOk { chat ->
                    log.i { "Chat created: ${chat.id}" }
                    navigator.push(Route.Chat(sessionCredentials.sessionId, chat.id))
                }.onErr { errorResponse ->
                    // TODO Show user-friendly error
                    log.e { "Failed to create chat: ${errorResponse.errorBody}" }
                }
        }
    }

    private fun onClickDeleteChat(
        sessionCredentials: SessionCredentials,
        chat: Chat,
    ) {
        viewModelScope.launch {
            chatRepository
                .delete(sessionCredentials.sessionId, chat.id)
                .onOk {
                    log.i { "Chat deleted: ${chat.id}" }
                }.onErr { errorResponse ->
                    // TODO Show user-friendly error
                    log.e { "Failed to delete chat: ${errorResponse.errorBody}" }
                }
        }
    }

    private fun onClickLogout() {
        replaceStreamChatsJob("Logging out", work = null)
        viewModelScope.launch {
            // TODO Handle failure to log out?
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
    }
}
