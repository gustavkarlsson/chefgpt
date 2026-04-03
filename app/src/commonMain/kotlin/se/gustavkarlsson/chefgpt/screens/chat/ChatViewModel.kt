package se.gustavkarlsson.chefgpt.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
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
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ConversationFactory
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.JoinId
import se.gustavkarlsson.chefgpt.chats.Conversation
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${ChatViewModel::class.simpleName}")

// TODO Fix error handling
class ChatViewModel(
    private val client: ChefGptClient,
    conversationFactory: ConversationFactory,
    private val navigator: Navigator,
    @InjectedParam private val chat: Route.Chat,
) : ViewModel() {
    private val conversation: Conversation = conversationFactory.create(chat.sessionId, chat.chatId)

    private data class State(
        val joinId: JoinId? = null,
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
        val onClickBack: () -> Unit,
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
                    log.i(e) { "Session interrupted" }
                } finally {
                    runCatching { stopSession() }
                    delay(1.seconds)
                }
            }
        }
    }

    private fun State.toViewState(): ViewState =
        ViewState(
            connected = joinId != null,
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
            onClickBack = { navigator.pop() },
        )

    private fun State.allowsSend(): Boolean =
        when {
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
            log.i { "Sending message to ${conversation.chatId}" }

            if (lastState.attachedImage != null) {
                val extension = lastState.attachedImage.toString().substringAfterLast(".")
                // TODO Introduce user-case
                client.uploadImage(conversation.sessionId, lastState.attachedImage, ContentType("image", extension))
            } else {
                Ok(null)
            }.map { imageUrl ->
                conversation.send(ApiUserSendsMessage(lastState.userText, imageUrl))
            }.onErr { errorResponse ->
                // TODO Show message?
                //  Modify state?
                log.i { "Failed to send message: ${errorResponse.errorBody}" }
            }
        }
    }

    private suspend fun runSession() =
        coroutineScope {
            val joinId = JoinId.random()
            innerState.update { it.copy(joinId = joinId) }

            val job =
                launch {
                    // TODO Handle errors
                    conversation.streamEvents().collect { event ->
                        innerState.update { it.copy(events = it.events + event) }
                    }
                }
            conversation.send(ApiUserJoinedChat(joinId))
            job.join()
        }

    private fun stopSession() {
        innerState.update {
            it.copy(joinId = null)
        }
    }
}
