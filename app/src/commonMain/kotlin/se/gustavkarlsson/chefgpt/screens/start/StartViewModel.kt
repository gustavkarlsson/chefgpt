package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.displayName
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
    private val streamChatsJob = atomic<Job?>(null)

    private val innerState = MutableStateFlow(State())

    val uiState: StateFlow<UiState> =
        innerState
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), innerState.value.toUiState())

    private fun State.toUiState(): UiState =
        UiState(
            content = toContent(),
            onClickDebug = ::openDebug,
        )

    private fun State.toContent(): UiState.Content =
        when {
            !initialized -> {
                UiState.Content.Loading
            }

            sessionCredentials == null -> {
                UiState.Content.LoggedOut(
                    username = inputUsername,
                    password = inputPassword,
                    onUsernameChange = ::updateUsername,
                    onPasswordChange = ::updatePassword,
                    onClickRegister = if (canAuthenticate) ::register else null,
                    onClickLogin = if (canAuthenticate) ::logIn else null,
                )
            }

            else -> {
                UiState.Content.LoggedIn(
                    username = sessionCredentials.username.value,
                    chats = chats.toUiChats(),
                    onClickNewChat = ::createChat,
                    onClickIngredients = ::openIngredients,
                    onClickLogout = ::logOut,
                )
            }
        }

    private val State.canAuthenticate: Boolean
        get() = inputUsername.isNotBlank() && inputPassword.isNotBlank() && !authenticating

    private fun List<Chat>.toUiChats(): List<UiChat> =
        map { chat ->
            UiChat(
                id = chat.id,
                title = chat.displayName,
                onClick = ::openChat,
                onClickDelete = ::deleteChat,
            )
        }

    init {
        viewModelScope.launch {
            // Ignore errors, as we can just start with a fresh session
            sessionRepository
                .getCurrentSession()
                .onOk { credentials ->
                    if (credentials != null) {
                        innerState.update { it.copy(sessionCredentials = credentials) }
                        restartChatStream(credentials)
                    }
                }
            innerState.update { it.copy(initialized = true) }
        }
    }

    private fun updateUsername(username: String) {
        innerState.update { it.copy(inputUsername = username) }
    }

    private fun updatePassword(password: String) {
        innerState.update { it.copy(inputPassword = password) }
    }

    private fun register() {
        val state = innerState.value
        if (state.authenticating) return
        val username = state.inputUsername
        innerState.update { it.copy(authenticating = true) }
        viewModelScope.launch {
            try {
                sessionRepository
                    .register(state.inputCredentials)
                    .onOk { onAuthenticated(username, it, "Registered") }
                    .onErr { error ->
                        // TODO Show correct feedback message based on the error
                        when (error) {
                            is RegisterError.ServerError -> {
                                log.i { "Registration failed for '$username': ${error.error}" }
                            }

                            RegisterError.StorageFailed -> {
                                log.e { "Registration succeeded but failed to save session for '$username'" }
                            }
                        }
                    }
            } finally {
                innerState.update { it.copy(authenticating = false) }
            }
        }
    }

    private fun logIn() {
        val state = innerState.value
        if (state.authenticating) return
        val username = state.inputUsername
        innerState.update { it.copy(authenticating = true) }
        viewModelScope.launch {
            try {
                sessionRepository
                    .login(state.inputCredentials)
                    .onOk { onAuthenticated(username, it, "Logged in") }
                    .onErr {
                        // TODO Show correct feedback message based on the status code
                        log.i { "Login failed for '$username': $it" }
                    }
            } finally {
                innerState.update { it.copy(authenticating = false) }
            }
        }
    }

    private fun onAuthenticated(
        username: String,
        credentials: SessionCredentials,
        action: String,
    ) {
        log.i { "$action as '$username'" }
        innerState.update { it.copy(sessionCredentials = credentials) }
        restartChatStream(credentials)
    }

    private fun createChat() {
        val credentials = innerState.value.sessionCredentials ?: return
        viewModelScope.launch {
            chatRepository
                .create(credentials.sessionId)
                .onOk { chat ->
                    log.i { "Chat created: ${chat.id}" }
                    navigator.push(Route.Chat(credentials.sessionId, chat.id))
                }.onErr {
                    // TODO Show user-friendly error
                    log.e { "Failed to create chat: $it" }
                }
        }
    }

    private fun openChat(chatId: ChatId) {
        val credentials = innerState.value.sessionCredentials ?: return
        navigator.push(Route.Chat(credentials.sessionId, chatId))
    }

    private fun deleteChat(chatId: ChatId) {
        val credentials = innerState.value.sessionCredentials ?: return
        viewModelScope.launch {
            chatRepository
                .delete(credentials.sessionId, chatId)
                .onOk { log.i { "Chat deleted: $chatId" } }
                .onErr {
                    // TODO Show user-friendly error
                    log.e { "Failed to delete chat: $it" }
                }
        }
    }

    private fun openIngredients() {
        val credentials = innerState.value.sessionCredentials ?: return
        navigator.push(Route.Ingredients(credentials.sessionId))
    }

    private fun openDebug() {
        navigator.push(Route.Debug)
    }

    private fun logOut() {
        restartChatStream(credentials = null)
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

    private fun restartChatStream(credentials: SessionCredentials?) {
        val job = credentials?.let { creds -> viewModelScope.launch { streamChats(creds) } }
        streamChatsJob.getAndSet(job)?.cancel()
    }

    private suspend fun streamChats(credentials: SessionCredentials) {
        chatRepository
            .stream(credentials.sessionId)
            .collect { chats -> innerState.update { it.copy(chats = chats) } }
    }
}

private data class State(
    val initialized: Boolean = false,
    val sessionCredentials: SessionCredentials? = null,
    val chats: List<Chat> = emptyList(),
    val inputUsername: String = "",
    val inputPassword: String = "",
    val authenticating: Boolean = false,
) {
    val inputCredentials: UserCredentials
        get() = UserCredentials(inputUsername, inputPassword)
}

data class UiState(
    val content: Content,
    val onClickDebug: () -> Unit,
) {
    sealed interface Content {
        data object Loading : Content

        data class LoggedOut(
            val username: String,
            val password: String,
            val onUsernameChange: (String) -> Unit,
            val onPasswordChange: (String) -> Unit,
            val onClickRegister: (() -> Unit)?,
            val onClickLogin: (() -> Unit)?,
        ) : Content

        data class LoggedIn(
            val username: String,
            val chats: List<UiChat>,
            val onClickNewChat: () -> Unit,
            val onClickIngredients: () -> Unit,
            val onClickLogout: () -> Unit,
        ) : Content
    }
}

data class UiChat(
    val id: ChatId,
    val title: String,
    val onClick: (ChatId) -> Unit,
    val onClickDelete: (ChatId) -> Unit,
)
