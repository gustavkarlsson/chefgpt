package se.gustavkarlsson.chefgpt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.api.ApiAction
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import kotlin.uuid.Uuid

// TODO Fix error handling
class FindRecipeViewModel : ViewModel() {
    private val client = ChefGptClient()

    private val joinEvent = ApiUserJoinedChat(Uuid.random())

    private data class State(
        val chatId: ChatId?,
        val events: List<ApiEvent>,
        val userText: String,
        val attachedImage: File?,
    )

    // TODO Don't make inner, but make it data
    inner class ViewState(
        val actions: List<ApiAction>,
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

    private val state =
        MutableStateFlow(
            State(
                chatId = null,
                events = emptyList(),
                userText = "",
                attachedImage = null,
            ),
        )

    val viewState: StateFlow<ViewState> =
        state
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toViewState())

    private fun State.toViewState(): ViewState =
        ViewState(
            actions = events.filterIsInstance<ApiAction>(),
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
            chatId == null -> {
                false
            }

            // No chat yet
            userText.isBlank() && attachedImage == null -> {
                false
            }

            // Nothing to sent
            joinEvent !in events -> {
                false
            }

            // Haven't seen the join event yet
            else -> {
                val lastAction = events.asSequence().filterIsInstance<ApiAction>().lastOrNull()
                lastAction == null || lastAction is ApiAgentMessage
            }
        }

    private fun sendMessage() {
        viewModelScope.launch {
            // TODO what prevents the user from quickly sending two messages in a row?
            //  Do we need to update some kind of "waiting" state?
            val lastState =
                state.getAndUpdate {
                    it.copy(userText = "", attachedImage = null)
                }
            val imageUrl =
                lastState.attachedImage?.let { file ->
                    val extension = file.path.substringAfterLast(".")
                    client.uploadImage(file.readChannel(), ContentType("image", extension))
                }
            client.sendEvent(lastState.chatId!!, ApiUserMessage(lastState.userText, imageUrl))
        }
    }

    init {
        viewModelScope.launch {
            client.register()
            val chatId = client.createChat()
            state.update { it.copy(chatId = chatId) }
            launch {
                client.listenToEvents(chatId).collect { event ->
                    state.update { it.copy(events = it.events + event) }
                }
            }
            client.sendEvent(chatId, joinEvent)
        }
    }
}
