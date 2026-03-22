package se.gustavkarlsson.chefgpt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.http.ContentType
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.api.Action
import se.gustavkarlsson.chefgpt.api.UserMessage

class FindRecipeViewModel : ViewModel() {
    private val client = ChefGptClient()
    private val conversation =
        viewModelScope.async {
            client.register()
            val chatId = client.createChat()
            joinConversation(client, chatId)
        }

    private suspend fun conversation() = conversation.await()

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

    private data class State(
        val acceptingInput: Boolean,
        val actions: List<Action>,
        val userText: String,
        val attachedImage: File?,
    )

    private val state =
        MutableStateFlow(
            State(
                acceptingInput = false,
                actions = emptyList(),
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
            actions = actions,
            userText = userText,
            attachedImage = attachedImage,
            onClickSend =
                if (acceptingInput && userText.isNotBlank()) {
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
            val imageUrl =
                lastState.attachedImage?.let { file ->
                    val extension = file.path.substringAfterLast(".")
                    client.uploadImage(file.readChannel(), ContentType("image", extension))
                }
            conversation().sendToAgent(UserMessage(lastState.userText, imageUrl))
        }
    }

    init {
        viewModelScope.launch {
            conversation().acceptingInput.collect { accept ->
                state.update { it.copy(acceptingInput = accept) }
            }
        }

        viewModelScope.launch {
            conversation().actionHistory.collect { action ->
                state.update { it.copy(actions = it.actions + action) }
            }
        }
    }
}
