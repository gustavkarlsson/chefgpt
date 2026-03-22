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
import se.gustavkarlsson.chefgpt.api.Action
import se.gustavkarlsson.chefgpt.api.AgentMessage
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.UserJoinedChat
import se.gustavkarlsson.chefgpt.api.UserMessage
import kotlin.uuid.Uuid

// TODO Fix error handling
class FindRecipeViewModel : ViewModel() {
    private val client = ChefGptClient()

    private val joinEvent = UserJoinedChat(Uuid.random())

    private data class State(
        val chatId: ChatId?,
        val events: List<Event>,
        val userText: String,
        val attachedImage: File?,
    )

    // TODO Don't make inner, but make it data
    inner class ViewState(
        val actions: List<Action>,
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
            actions = events.filterIsInstance<Action>(),
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
                val lastAction = events.asSequence().filterIsInstance<Action>().lastOrNull()
                lastAction == null || lastAction is AgentMessage
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
            client.sendEvent(lastState.chatId!!, UserMessage(lastState.userText, imageUrl))
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
