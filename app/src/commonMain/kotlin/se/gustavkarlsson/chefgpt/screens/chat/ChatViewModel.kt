package se.gustavkarlsson.chefgpt.screens.chat

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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
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
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsScreen
import se.gustavkarlsson.chefgpt.sessions.SessionId
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${ChatViewModel::class.simpleName}")

private val RECONNECT_DELAY = 1.seconds

private const val EMPTY_HEADLINE = "What are you cooking today?"
private const val EMPTY_DESCRIPTION =
    "Describe what you'd like help with and I'll lend a hand in the kitchen."
private val EXAMPLE_PROMPTS =
    listOf(
        "What can I make with chicken, rice, and broccoli?",
        "Suggest a quick vegetarian dinner for two",
        "I'm craving something sweet. What can I make with what I already have?",
        "How do I make fluffy pancakes from scratch?",
        "Give me a dessert idea using only pantry staples",
    )

// TODO Fix error handling
class ChatViewModel(
    private val client: ChefGptClient,
    conversationFactory: ConversationFactory,
    private val chatRepository: ChatRepository,
    private val navigator: Navigator,
    private val emojiResolverFactory: IngredientEmojiResolver.Factory,
    @InjectedParam private val screen: ChatScreen,
) : StateViewModel<State, UiState>() {
    private val sessionId: SessionId = screen.sessionId
    private val conversation: Conversation = conversationFactory.create(sessionId, screen.chatId)

    // One example prompt per chat, chosen deterministically from the chat ID so it stays stable.
    private val examplePrompt: String =
        EXAMPLE_PROMPTS.random(
            Random(
                conversation.chatId.value
                    .hashCode()
                    .toLong(),
            ),
        )

    override fun createInitialState() = State()

    // Discrete add/remove events that the UI animates one at a time; transient, so they live
    // outside UiState rather than as state.
    private val ingredientChangeChannel = Channel<IngredientChange>(Channel.UNLIMITED)
    val ingredientChanges: Flow<IngredientChange> = ingredientChangeChannel.receiveAsFlow()

    override fun State.toUiState(): UiState =
        UiState(
            title = chat?.displayName.orEmpty(),
            connected = isConnected(),
            content = toUiContent(),
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

    // No messages yet: invite the user to describe what they want help cooking, with examples.
    // The example prompts can be tapped to submit them, but only once we're joined and able to send.
    private fun State.toUiContent(): UiContent {
        val messages = events.toUiMessages()
        return if (messages.isEmpty()) {
            UiContent.Empty(
                headline = EMPTY_HEADLINE,
                description = EMPTY_DESCRIPTION,
                examplePrompt = examplePrompt,
                onClickPrompt = if (isJoined()) ::submitPrompt else null,
            )
        } else {
            UiContent.Messages(messages)
        }
    }

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

    // The join ID has been acknowledged by the backend, so actions can be sent.
    private fun State.isJoined(): Boolean = joinId in events.filterIsInstance<ApiUserJoined>().map { it.joinId }

    private fun State.canSend(): Boolean =
        when {
            !isJoined() -> false

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
        navigator.push(IngredientsScreen(sessionId))
    }

    private fun submitPrompt(prompt: String) {
        updateUserText(prompt)
        sendMessage()
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
            }.onErr { error ->
                log.e { "Failed to send message: $error" }
                showSnackbar("Couldn't send message", isError = true)
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
                            // Reconnecting replays the full history, so upsert by id to avoid
                            // duplicate events (and duplicate LazyColumn keys) on resume.
                            innerState.update { it.copy(events = it.events.upsert(event)) }
                        }.onErr { errorResponse ->
                            log.e { "Failed to stream events: $errorResponse" }
                        }
                }
            }
            conversation.sendAction(ApiUserJoinedChat(joinId))
            // Waits until the launch job is done
        }
}

// Replaces the event with the same id if present, preserving order; otherwise appends.
private fun List<ApiEvent>.upsert(event: ApiEvent): List<ApiEvent> {
    val index = indexOfFirst { it.id == event.id }
    return if (index < 0) this + event else toMutableList().also { it[index] = event }
}

data class State(
    val joinId: JoinId? = null,
    val chat: Chat? = null,
    val events: List<ApiEvent> = emptyList(),
    val userText: String = "",
    val attachedImage: Path? = null,
)

data class UiState(
    val title: String,
    val connected: Boolean,
    val content: UiContent,
    val input: UiInput,
    val onClickBack: () -> Unit,
    val onClickIngredients: () -> Unit,
)

sealed interface UiContent {
    data class Empty(
        val headline: String,
        val description: String,
        val examplePrompt: String,
        // Null until joined, so the prompt can't be submitted before we can send.
        val onClickPrompt: ((String) -> Unit)?,
    ) : UiContent

    data class Messages(
        val messages: List<UiMessage>,
    ) : UiContent
}

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
