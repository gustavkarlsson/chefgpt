package se.gustavkarlsson.chefgpt

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
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

// TODO Fix error handling
class FindRecipeViewModel : ViewModel() {
    private data class State(
        val client: ChefGptClient? = null,
        val chatId: ChatId? = null,
        val joinId: Uuid? = null,
        val events: List<ApiEvent> = emptyList(),
        val userText: String = "",
        val attachedImage: File? = null,
    )

    // TODO Don't make inner, but make it data
    inner class ViewState(
        val connected: Boolean,
        val events: List<ApiEvent>,
        val userText: String,
        val attachedImage: File?,
        val onClickSend: (() -> Unit)?,
        val onImageCleared: (() -> Unit)?,
    ) {
        val onUserTextChanged: (String) -> Unit
            get() = { text -> state.update { it.copy(userText = text) } }
        val onImageAttached: (File) -> Unit
            get() = { image -> state.update { it.copy(attachedImage = image) } }
    }

    private val state = MutableStateFlow(State())

    val viewState: StateFlow<ViewState> =
        state
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toViewState())

    init {
        viewModelScope.launch {
            while (true) {
                try {
                    runSession()
                } catch (e: CancellationException) {
                    // For good coroutine hygiene
                    throw e
                } catch (_: Exception) {
                    // Boo-hoo, session died
                } finally {
                    runCatching { stopSession() }
                    delay(1.seconds)
                }
            }
        }
    }

    private fun State.toViewState(): ViewState =
        ViewState(
            connected = client != null && chatId != null && joinId != null,
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
                    { state.update { it.copy(attachedImage = null) } }
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
                state.getAndUpdate {
                    it.copy(userText = "", attachedImage = null)
                }
            if (lastState.client == null || lastState.chatId == null) {
                return@launch
            }
            val imageUrl =
                lastState.attachedImage?.let { file ->
                    val extension = file.path.substringAfterLast(".")
                    lastState.client.uploadImage(file.readChannel(), ContentType("image", extension))
                }
            lastState.client.sendAction(lastState.chatId, ApiUserSendsMessage(lastState.userText, imageUrl))
        }
    }

    private suspend fun CoroutineScope.runSession() {
        val client = ChefGptClient()
        state.update { it.copy(client = client) }
        check(client.register()) {
            "Registration failed"
        }
        val joinId = Uuid.random()
        state.update { it.copy(joinId = joinId) }
        val chatId = client.createChat()
        state.update { it.copy(chatId = chatId, events = emptyList()) }
        val listenJob =
            launch {
                client.listenToEvents(chatId).collect { event ->
                    state.update { it.copy(events = it.events + event) }
                }
            }
        client.sendAction(chatId, ApiUserJoinedChat(joinId))
        listenJob.join()
    }

    private fun stopSession() {
        state.update {
            it.client?.close()
            it.copy(client = null, chatId = null, joinId = null)
        }
    }
}
