package se.gustavkarlsson.chefgpt.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
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
    private val client: ChefGptClient,
    private val sessionId: SessionId,
) : ViewModel() {
    private data class State(
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
            connected = chatId != null && joinId != null,
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
            if (lastState.chatId == null) {
                return@launch
            }

            val imageUrl =
                if (lastState.attachedImage != null) {
                    val extension = lastState.attachedImage.toString().substringAfterLast(".")
                    client.uploadImage(sessionId, lastState.attachedImage, ContentType("image", extension))
                } else {
                    Ok(null)
                }.map { imageUrl ->
                    client.sendAction(sessionId, lastState.chatId, ApiUserSendsMessage(lastState.userText, imageUrl))
                }.onErr {
                    // TODO Show message?
                    //  Modify state?
                }
        }
    }

    private suspend fun runSession() =
        coroutineScope {
            val joinId = Uuid.random()
            innerState.update { it.copy(joinId = joinId) }
            // TODO Add ability to resume chat
            var job: Job? = null
            client
                .createChat(sessionId)
                .onOk { chatId ->
                    innerState.update { it.copy(chatId = chatId, events = emptyList()) }
                    job =
                        launch {
                            // TODO Handle errors
                            client.listenToEvents(sessionId, chatId).collect { event ->
                                innerState.update { it.copy(events = it.events + event) }
                            }
                        }
                }.map { chatId ->
                    client.sendAction(sessionId, chatId, ApiUserJoinedChat(joinId))
                }.onErr {
                    // TODO Show message?
                    //  Modify state?
                    job?.cancel("Failed to run session")
                }
            job?.join()
        }

    private fun stopSession() {
        innerState.update {
            it.copy(chatId = null, joinId = null)
        }
    }
}
