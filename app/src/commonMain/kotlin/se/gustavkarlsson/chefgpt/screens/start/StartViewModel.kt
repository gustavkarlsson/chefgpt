package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
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
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.StoredChat
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.UserCredentials
import se.gustavkarlsson.chefgpt.sessions.UserName

private val log = Logger.withTag("${StartViewModel::class.simpleName}")

class StartViewModel(
    private val sessionRepository: SessionRepository,
    private val chatRepository: ChatRepository,
    private val client: ChefGptClient,
    private val navigator: Navigator,
) : ViewModel() {
    private data class State(
        val username: UserName? = null,
        val sessionId: SessionId? = null,
        val chats: List<StoredChat> = emptyList(),
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
            val chats: List<StoredChat>,
            val onClickNewChat: () -> Unit,
            val onClickChat: (StoredChat) -> Unit,
            val onClickLogout: () -> Unit,
        ) : ViewState
    }

    private val innerState = MutableStateFlow(State())

    // TODO Introduce a loading state before we know if we're logged in or not
    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    init {
        viewModelScope.launch {
            val credentials = sessionRepository.getCurrentSession()
            if (credentials != null) {
                log.i { "Restored session for '${credentials.username}'" }
                val chats =
                    chatRepository
                        .loadAll()
                        .filter { it.owner == credentials.username }
                        .sortedByDescending { it.createdAt }
                innerState.value =
                    State(
                        username = credentials.username,
                        sessionId = credentials.sessionId,
                        chats = chats,
                    )
            }
        }
    }

    fun refreshChats() {
        val state = innerState.value
        val username = state.username ?: return
        val chats =
            chatRepository
                .loadAll()
                .filter { it.owner == username }
                .sortedByDescending { it.createdAt }
        innerState.update { it.copy(chats = chats) }
    }

    private fun State.toViewState(): ViewState =
        if (username != null && sessionId != null) {
            ViewState.LoggedIn(
                username = username.value,
                chats = chats,
                onClickNewChat = {
                    viewModelScope.launch {
                        // TODO Create chat with chatrepository
                        client
                            .createChat(sessionId)
                            .onOk { apiChat ->
                                log.i { "Chat created: ${apiChat.id}" }
                                chatRepository.save(StoredChat(apiChat.id, apiChat.createdAt, username))
                                innerState.update {
                                    it.copy(
                                        chats =
                                            listOf(StoredChat(apiChat.id, apiChat.createdAt, username)) + it.chats,
                                    )
                                }
                                navigator.push(Route.Chat(sessionId, apiChat))
                            }.onErr { errorResponse ->
                                // TODO Show message?
                                log.i { "Failed to create chat: ${errorResponse.errorBody}" }
                            }
                    }
                },
                onClickChat = { storedChat ->
                    navigator.push(Route.Chat(sessionId, ApiChat(storedChat.id, storedChat.createdAt)))
                },
                onClickLogout = {
                    log.i { "Logging out '$username'" }
                    viewModelScope.launch {
                        sessionRepository.clearCurrentSession()
                    }
                    innerState.update {
                        it.copy(
                            username = null,
                            sessionId = null,
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
                                    .onOk { session ->
                                        log.i { "Registered as '$inputUsername'" }
                                        innerState.update {
                                            it.copy(
                                                username = UserName(inputUsername),
                                                sessionId = session.sessionId,
                                            )
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
                                    .onOk { session ->
                                        log.i { "Logged in as '$inputUsername'" }
                                        innerState.update {
                                            it.copy(
                                                username = UserName(inputUsername),
                                                sessionId = session.sessionId,
                                            )
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
