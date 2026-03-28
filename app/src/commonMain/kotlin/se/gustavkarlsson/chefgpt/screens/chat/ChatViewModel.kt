package se.gustavkarlsson.chefgpt.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.SessionRepository
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.SessionId
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

// TODO Fix error handling
class ChatViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private data class State(
        val client: ChefGptClient? = null,
        val sessionId: SessionId? = null,
        val chatId: ChatId? = null,
        val joinId: Uuid? = null,
        val events: List<ApiEvent> = emptyList(),
        val userText: String = "",
        val attachedImage: Path? = null,
    )

    // TODO Don't make inner, but make it data
    inner class ViewState(
        val connected: Boolean,
        val events: List<ApiEvent>,
        val userText: String,
        val attachedImage: Path?,
        val onClickSend: (() -> Unit)?,
        val onImageCleared: (() -> Unit)?,
    ) {
        val onUserTextChanged: (String) -> Unit
            get() = { text -> innerState.update { it.copy(userText = text) } }
        val onImageAttached: (Path) -> Unit
            get() = { image -> innerState.update { it.copy(attachedImage = image) } }
    }

    private val innerState = MutableStateFlow(State())

    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    init {
        viewModelScope.launch {
            while (true) {
                try {
                    runSession()
                } catch (e: CancellationException) {
                    // For good coroutine hygiene
                    throw e
                } catch (e: Exception) {
                    // TODO Logging
                    println(e.message)
                } finally {
                    runCatching { stopSession() }
                    delay(1.seconds)
                }
            }
        }
    }

    private fun State.toViewState(): ViewState =
        ViewState(
            connected = client != null && sessionId != null && chatId != null && joinId != null,
            events = events,
            userText = userText,
            attachedImage = attachedImage,
            onClickSend =
                if (allowsSend() && userText.isNotBlank()) {
                    ::sendMessage
                } else {
                    null
                },
            onImageCleared =
                if (attachedImage != null) {
                    { innerState.update { it.copy(attachedImage = null) } }
                } else {
                    null
                },
        )

    private fun State.allowsSend(): Boolean =
        when {
            // No chat yet
            chatId == null -> false

            // Nothing to sent
            userText.isBlank() && attachedImage == null -> false

            // Has the join ID been received?
            else -> joinId in events.filterIsInstance<ApiUserJoined>().map { it.joinId }
        }

    private fun sendMessage() {
        viewModelScope.launch {
            // TODO what prevents the user from quickly sending two messages in a row?
            //  Do we need to update some kind of "waiting" state?
            val lastState =
                innerState.getAndUpdate {
                    it.copy(userText = "", attachedImage = null)
                }
            if (lastState.client == null || lastState.sessionId == null || lastState.chatId == null) {
                return@launch
            }
            val imageUrl =
                lastState.attachedImage?.let { path ->
                    val extension = path.toString().substringAfterLast(".")
                    lastState.client.uploadImage(lastState.sessionId, path, ContentType("image", extension))
                }
            lastState.client.sendAction(
                lastState.sessionId,
                lastState.chatId,
                ApiUserSendsMessage(lastState.userText, imageUrl),
            )
        }
    }

    private suspend fun CoroutineScope.runSession() {
        val client = ChefGptClient()
        innerState.update { it.copy(client = client) }
        // FIXME Handle missing session gracefully (e.g. navigate to login screen)
        val sessionId = checkNotNull(sessionRepository.load()) { "No session found" }
        innerState.update { it.copy(sessionId = sessionId) }
        val joinId = Uuid.random()
        innerState.update { it.copy(joinId = joinId) }
        val chatId = client.createChat(sessionId)
        innerState.update { it.copy(chatId = chatId, events = emptyList()) }
        val listenJob =
            launch {
                client.listenToEvents(sessionId, chatId).collect { event ->
                    innerState.update { it.copy(events = it.events + event) }
                }
            }
        client.sendAction(sessionId, chatId, ApiUserJoinedChat(joinId))
        listenJob.join()
    }

    private fun stopSession() {
        innerState.update {
            it.client?.close()
            it.copy(client = null, sessionId = null, chatId = null, joinId = null)
        }
    }
}
