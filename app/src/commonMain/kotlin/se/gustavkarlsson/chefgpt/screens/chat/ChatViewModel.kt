package se.gustavkarlsson.chefgpt.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.ApiAgentChatNamed
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiAgentReasoning
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.ApiSystemEvent
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.JoinId
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.Conversation
import se.gustavkarlsson.chefgpt.chats.ConversationFactory
import se.gustavkarlsson.chefgpt.chats.displayName
import se.gustavkarlsson.chefgpt.ingredients.EmojiAvatarModel
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.sessions.SessionId
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${ChatViewModel::class.simpleName}")

private val RECONNECT_DELAY = 1.seconds

// TODO Fix error handling
class ChatViewModel(
    private val client: ChefGptClient,
    conversationFactory: ConversationFactory,
    private val chatRepository: ChatRepository,
    private val navigator: Navigator,
    private val emojiResolverFactory: IngredientEmojiResolver.Factory,
    @InjectedParam private val route: Route.Chat,
) : ViewModel() {
    private val sessionId: SessionId = route.sessionId
    private val conversation: Conversation = conversationFactory.create(sessionId, route.chatId)

    private val innerState = MutableStateFlow(State())

    val uiState: StateFlow<UiState> =
        innerState
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), innerState.value.toUiState())

    // Discrete add/remove events that the UI animates one at a time; transient, so they live
    // outside UiState rather than as state.
    private val ingredientChangeChannel = Channel<IngredientChange>(Channel.UNLIMITED)
    val ingredientChanges: Flow<IngredientChange> = ingredientChangeChannel.receiveAsFlow()

    private fun State.toUiState(): UiState =
        UiState(
            title = chat?.displayName.orEmpty(),
            connected = isConnected(),
            messages = events.toUiMessages(),
            input =
                UiInput(
                    text = userText,
                    attachedImage = attachedImage,
                    onTextChanged = ::updateUserText,
                    onImageAttached = ::attachImage,
                    onClickClearImage = if (attachedImage != null) ::clearImage else null,
                    onClickSend = if (canSend()) ::sendMessage else null,
                ),
            onClickBack = navigator::pop,
            onClickIngredients = ::openIngredients,
        )

    private fun List<ApiEvent>.toUiMessages(): List<UiMessage> =
        mapNotNull { event ->
            when (event) {
                is ApiSystemEvent -> null
                is ApiAgentChatNamed -> null
                is ApiAgentMessage -> UiMessage.Agent(event.id.toString(), event.text, reasoning = false)
                is ApiAgentReasoning -> UiMessage.Agent(event.id.toString(), event.text, reasoning = true)
                is ApiUserMessage -> UiMessage.User(event.id.toString(), event.text, event.imageUrl?.toString())
            }
        }

    private fun State.isConnected(): Boolean = joinId != null

    private fun State.canSend(): Boolean =
        when {
            // Not yet connected: the join ID hasn't been acknowledged by the backend.
            joinId !in events.filterIsInstance<ApiUserJoined>().map { it.joinId } -> false

            // Nothing to send.
            else -> userText.isNotBlank() || attachedImage != null
        }

    init {
        viewModelScope.launch {
            chatRepository.stream(sessionId).collect { chats ->
                val chat = chats.firstOrNull { it.id == conversation.chatId }
                innerState.update { it.copy(chat = chat) }
            }
        }
        viewModelScope.launch {
            val emojiResolver = emojiResolverFactory.create()
            // Skip the first emission so the initial inventory doesn't flash as changes.
            var previous: List<ApiIngredient>? = null
            client.listenToIngredients(sessionId).collect { ingredients ->
                val current = ingredients.filter { it.inInventory }
                previous?.let { prev ->
                    val previousIds = prev.map { it.id }.toSet()
                    val currentIds = current.map { it.id }.toSet()
                    current
                        .filter { it.id !in previousIds }
                        .forEach {
                            ingredientChangeChannel.send(
                                IngredientChange.Added(EmojiAvatarModel.of(emojiResolver.resolve(it.name), it.name)),
                            )
                        }
                    prev
                        .filter { it.id !in currentIds }
                        .forEach {
                            ingredientChangeChannel.send(
                                IngredientChange.Removed(EmojiAvatarModel.of(emojiResolver.resolve(it.name), it.name)),
                            )
                        }
                }
                previous = current
            }
        }
        viewModelScope.launch {
            while (true) {
                try {
                    runSession()
                    log.e { "Session ended" }
                } catch (e: CancellationException) {
                    // For good coroutine hygiene
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Session failed" }
                } finally {
                    innerState.update { it.copy(joinId = null) }
                    delay(RECONNECT_DELAY)
                }
            }
        }
    }

    private fun updateUserText(text: String) {
        innerState.update { it.copy(userText = text) }
    }

    private fun attachImage(image: Path) {
        innerState.update { it.copy(attachedImage = image) }
    }

    private fun clearImage() {
        innerState.update { it.copy(attachedImage = null) }
    }

    private fun openIngredients() {
        navigator.push(Route.Ingredients(sessionId))
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
                // TODO Introduce use-case
                client.uploadImage(sessionId, lastState.attachedImage, ContentType("image", extension))
            } else {
                Ok(null)
            }.map { imageUrl ->
                conversation.sendAction(ApiUserSendsMessage(lastState.userText, imageUrl))
            }.onErr { errorResponse ->
                // TODO Show message?
                //  Modify state?
                log.e { "Failed to send message: ${errorResponse.errorBody}" }
            }
        }
    }

    private suspend fun runSession() =
        coroutineScope {
            val joinId = JoinId.random()
            innerState.update { it.copy(joinId = joinId) }

            launch {
                conversation.events().collect { eventResult ->
                    eventResult
                        .onOk { event ->
                            innerState.update { it.copy(events = it.events + event) }
                        }.onErr { errorResponse ->
                            log.e { "Failed to stream events: $errorResponse" }
                        }
                }
            }
            conversation.sendAction(ApiUserJoinedChat(joinId))
            // Waits until the launch job is done
        }
}

private data class State(
    val joinId: JoinId? = null,
    val chat: Chat? = null,
    val events: List<ApiEvent> = emptyList(),
    val userText: String = "",
    val attachedImage: Path? = null,
)

data class UiState(
    val title: String,
    val connected: Boolean,
    val messages: List<UiMessage>,
    val input: UiInput,
    val onClickBack: () -> Unit,
    val onClickIngredients: () -> Unit,
)

data class UiInput(
    val text: String,
    val attachedImage: Path?,
    val onTextChanged: (String) -> Unit,
    val onImageAttached: (Path) -> Unit,
    val onClickClearImage: (() -> Unit)?,
    val onClickSend: (() -> Unit)?,
)

sealed interface UiMessage {
    val id: String

    data class User(
        override val id: String,
        val text: String?,
        val imageUrl: String?,
    ) : UiMessage

    data class Agent(
        override val id: String,
        val text: String,
        val reasoning: Boolean,
    ) : UiMessage
}

sealed interface IngredientChange {
    val icon: EmojiAvatarModel

    data class Added(
        override val icon: EmojiAvatarModel,
    ) : IngredientChange

    data class Removed(
        override val icon: EmojiAvatarModel,
    ) : IngredientChange
}
