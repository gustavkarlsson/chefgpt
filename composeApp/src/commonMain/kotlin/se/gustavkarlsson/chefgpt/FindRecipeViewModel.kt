package se.gustavkarlsson.chefgpt

import ai.koog.utils.io.use
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FindRecipeViewModel : ViewModel() {
    private val conversation = ConversationService()
    private val aiAgent = createAiAgent(conversation)

    // TODO Don't make inner, but make it data
    inner class ViewState(
        val messages: List<Message>,
        val userText: String,
        val onClickSend: (() -> Unit)?,
        val onImageCleared: (() -> Unit)?
    ) {
        val onUserTextChanged: (String) -> Unit
            get() = { text -> _state.update { it.copy(userText = text) } }
        val onImageAttached: (String) -> Unit
            get() = { image -> _state.update { it.copy(attachedImage = image) } }
    }

    private data class State(
        val conversationState: ConversationState,
        val messages: List<Message>,
        val userText: String,
        val attachedImage: String?,
    )

    private val _state =
        MutableStateFlow(
            State(
                conversationState = conversation.state.value,
                messages = emptyList(),
                userText = "",
                attachedImage = null
            )
        )

    val viewState: StateFlow<ViewState> = _state
        .map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toViewState())

    private fun State.toViewState(): ViewState {

        return ViewState(
            messages = messages,
            userText = userText,
            onClickSend = if (conversationState == ConversationState.WaitingForUser && userText.isNotBlank()) {
                ::sendMessage
            } else {
                null
            },
            onImageCleared = if (attachedImage != null) {
                { _state.update { it.copy(attachedImage = null) } }
            } else {
                null
            }
        )
    }

    private fun sendMessage() {
        viewModelScope.launch {
            val lastState = _state.getAndUpdate {
                it.copy(userText = "", attachedImage = null)
            }
            conversation.sayToAi(MessageContent(lastState.userText, lastState.attachedImage))
        }
    }

    init {
        viewModelScope.launch {
            aiAgent.use { it.run(Unit) }
        }
        viewModelScope.launch {
            conversation.state.collect { conversationState ->
                _state.update { it.copy(conversationState = conversationState) }
            }
        }

        viewModelScope.launch {
            conversation.messageHistory.collect { message ->
                _state.update { it.copy(messages = it.messages + message) }
            }
        }
    }

}
