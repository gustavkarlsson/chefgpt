package se.gustavkarlsson.chefgpt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FindRecipeViewModel : ViewModel() {
    private val conversation = viewModelScope.async { startWebSocketConversation() }

    private suspend fun conversation() = conversation.await()

    // TODO Don't make inner, but make it data
    inner class ViewState(
        val messages: List<Message>,
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

    private data class State(
        val conversationState: ConversationState,
        val messages: List<Message>,
        val userText: String,
        val attachedImage: File?,
    )

    private val state =
        MutableStateFlow(
            State(
                conversationState = ConversationState.WaitingForAi,
                messages = emptyList(),
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
            messages = messages,
            userText = userText,
            attachedImage = attachedImage,
            onClickSend =
                if (conversationState == ConversationState.WaitingForUser && userText.isNotBlank()) {
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

    private fun sendMessage() {
        viewModelScope.launch {
            val lastState =
                state.getAndUpdate {
                    it.copy(userText = "", attachedImage = null)
                }
            conversation().sayToAi(UserMessage(lastState.userText, lastState.attachedImage))
        }
    }

    init {
        viewModelScope.launch {
            conversation().state.collect { conversationState ->
                state.update { it.copy(conversationState = conversationState) }
            }
        }

        viewModelScope.launch {
            conversation().messageHistory.collect { message ->
                state.update { it.copy(messages = it.messages + message) }
            }
        }
    }
}
