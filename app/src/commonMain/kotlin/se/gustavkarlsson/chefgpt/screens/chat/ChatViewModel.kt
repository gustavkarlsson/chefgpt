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
import org.kodein.emoji.Emoji
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.ApiUserJoined
import se.gustavkarlsson.chefgpt.api.ApiUserJoinedChat
import se.gustavkarlsson.chefgpt.api.ApiUserSendsMessage
import se.gustavkarlsson.chefgpt.api.JoinId
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.Conversation
import se.gustavkarlsson.chefgpt.chats.ConversationFactory
import se.gustavkarlsson.chefgpt.chats.displayName
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${ChatViewModel::class.simpleName}")

sealed interface IngredientChange {
    val name: String
    val emoji: Emoji?

    data class Added(
        override val name: String,
        override val emoji: Emoji?,
    ) : IngredientChange

    data class Removed(
        override val name: String,
        override val emoji: Emoji?,
    ) : IngredientChange
}

// TODO Fix error handling
class ChatViewModel(
    private val client: ChefGptClient,
    conversationFactory: ConversationFactory,
    private val chatRepository: ChatRepository,
    private val navigator: Navigator,
    private val emojiResolverFactory: IngredientEmojiResolver.Factory,
    @InjectedParam private val route: Route.Chat,
) : ViewModel() {
    private val conversation: Conversation = conversationFactory.create(route.sessionId, route.chatId)

    private data class State(
        val joinId: JoinId? = null,
        val chat: Chat? = null,
        val events: List<ApiEvent> = emptyList(),
        val userText: String = "",
        val attachedImage: Path? = null,
    )

    // TODO Don't make inner, but make it data
    inner class ViewState(
        val connected: Boolean,
        val name: String,
        val events: List<ApiEvent>,
        val userText: String,
        val attachedImage: Path?,
        val onClickSend: (() -> Unit)?,
        val onImageCleared: (() -> Unit)?,
        val onClickBack: () -> Unit,
        val onClickIngredients: () -> Unit,
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

    private val ingredientChangeChannel = Channel<IngredientChange>(Channel.UNLIMITED)
    val ingredientChanges: Flow<IngredientChange> = ingredientChangeChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            chatRepository.stream(conversation.sessionId).collect { chats ->
                val chat = chats.firstOrNull { it.id == conversation.chatId }
                innerState.update { it.copy(chat = chat) }
            }
        }
        viewModelScope.launch {
            val emojiResolver = emojiResolverFactory.create()
            // Skip the first emission so the initial inventory doesn't flash as changes.
            var previous: List<ApiIngredient>? = null
            client.listenToIngredients(conversation.sessionId).collect { ingredients ->
                val current = ingredients.filter { it.inInventory }
                previous?.let { prev ->
                    val previousIds = prev.map { it.id }.toSet()
                    val currentIds = current.map { it.id }.toSet()
                    current
                        .filter { it.id !in previousIds }
                        .forEach {
                            ingredientChangeChannel.send(
                                IngredientChange.Added(it.name, emojiResolver.resolve(it.name)),
                            )
                        }
                    prev
                        .filter { it.id !in currentIds }
                        .forEach {
                            ingredientChangeChannel.send(
                                IngredientChange.Removed(it.name, emojiResolver.resolve(it.name)),
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
                    delay(1.seconds)
                }
            }
        }
    }

    private fun State.toViewState(): ViewState =
        ViewState(
            connected = joinId != null,
            name = chat?.displayName.orEmpty(),
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
            onClickIngredients = { navigator.push(Route.Ingredients(route.sessionId)) },
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
                // TODO Introduce use-case
                client.uploadImage(conversation.sessionId, lastState.attachedImage, ContentType("image", extension))
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
