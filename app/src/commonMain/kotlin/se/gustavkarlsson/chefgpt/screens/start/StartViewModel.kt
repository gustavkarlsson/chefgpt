package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.chats.Chat
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.displayName
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.recipes.RecipeId
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.RecipeSummary
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.screens.chat.ChatScreen
import se.gustavkarlsson.chefgpt.screens.debug.DebugScreen
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsScreen
import se.gustavkarlsson.chefgpt.screens.recipe.RecipeDetailScreen
import se.gustavkarlsson.chefgpt.sessions.RegisterError
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.UserCredentials

private val log = Logger.withTag("${StartViewModel::class.simpleName}")

class StartViewModel(
    private val chatRepository: ChatRepository,
    private val recipeRepository: RecipeRepository,
    private val sessionRepository: SessionRepository,
    private val navigator: Navigator,
) : StateViewModel<State, UiState>() {
    private val streamChatsJob = atomic<Job?>(null)
    private val streamRecipesJob = atomic<Job?>(null)

    override fun createInitialState() = State()

    override fun State.toUiState(): UiState =
        UiState(
            content = toContent(),
            onClickDebug = ::openDebug,
        )

    private fun State.toContent(): UiState.Content =
        when {
            !initialized -> {
                UiState.Content.Loading
            }

            sessionCredentials == null -> {
                UiState.Content.LoggedOut(
                    username = inputUsername,
                    password = inputPassword,
                    onUsernameChange = ::updateUsername,
                    onPasswordChange = ::updatePassword,
                    onClickRegister = if (canAuthenticate) ::register else null,
                    onClickLogin = if (canAuthenticate) ::logIn else null,
                )
            }

            else -> {
                UiState.Content.LoggedIn(
                    username = sessionCredentials.username.value,
                    chats = chats.toUiChats(),
                    recipeSummaries = recipeSummaries.toUiRecipeSummaries(),
                    onClickNewChat = ::createChat,
                    onClickIngredients = ::openIngredients,
                    onClickLogout = ::logOut,
                )
            }
        }

    private val State.canAuthenticate: Boolean
        get() = inputUsername.isNotBlank() && inputPassword.isNotBlank() && !authenticating

    private fun List<Chat>.toUiChats(): List<UiChat> =
        map { chat ->
            UiChat(
                id = chat.id,
                title = chat.displayName,
                onClick = ::openChat,
                onClickDelete = ::deleteChat,
            )
        }

    private fun List<RecipeSummary>.toUiRecipeSummaries(): List<UiRecipeSummary> =
        map { summary ->
            UiRecipeSummary(
                id = summary.id,
                title = summary.title,
                imageUrl = summary.imageUrl,
                onClickOpen = ::openRecipe,
                onClickDelete = ::deleteRecipe,
            )
        }

    init {
        viewModelScope.launch {
            // Ignore errors, as we can just start with a fresh session
            sessionRepository
                .getCurrentSession()
                .onOk { credentials ->
                    if (credentials != null) {
                        innerState.update { it.copy(sessionCredentials = credentials) }
                        restartChatStream(credentials)
                        restartRecipeStream(credentials)
                    }
                }
            innerState.update { it.copy(initialized = true) }
        }
    }

    private fun updateUsername(username: String) {
        innerState.update { it.copy(inputUsername = username) }
    }

    private fun updatePassword(password: String) {
        innerState.update { it.copy(inputPassword = password) }
    }

    private fun register() {
        val state = innerState.value
        if (state.authenticating) return
        val username = state.inputUsername
        innerState.update { it.copy(authenticating = true) }
        viewModelScope.launch {
            try {
                sessionRepository
                    .register(state.inputCredentials)
                    .onOk { onAuthenticated(username, it, "Registered") }
                    .onErr { error ->
                        when (error) {
                            is RegisterError.ServerError -> {
                                log.i { "Registration failed for '$username': ${error.error}" }
                                showSnackbar("Registration failed", isError = true)
                            }

                            RegisterError.StorageFailed -> {
                                log.e { "Registration succeeded but failed to save session for '$username'" }
                                showSnackbar("Couldn't save your session", isError = true)
                            }
                        }
                    }
            } finally {
                innerState.update { it.copy(authenticating = false) }
            }
        }
    }

    private fun logIn() {
        val state = innerState.value
        if (state.authenticating) return
        val username = state.inputUsername
        innerState.update { it.copy(authenticating = true) }
        viewModelScope.launch {
            try {
                sessionRepository
                    .login(state.inputCredentials)
                    .onOk { onAuthenticated(username, it, "Logged in") }
                    .onErr {
                        log.i { "Login failed for '$username': $it" }
                        showSnackbar("Login failed", isError = true)
                    }
            } finally {
                innerState.update { it.copy(authenticating = false) }
            }
        }
    }

    private fun onAuthenticated(
        username: String,
        credentials: SessionCredentials,
        action: String,
    ) {
        log.i { "$action as '$username'" }
        innerState.update { it.copy(sessionCredentials = credentials) }
        restartChatStream(credentials)
        restartRecipeStream(credentials)
    }

    private fun createChat() {
        val credentials = innerState.value.sessionCredentials ?: return
        viewModelScope.launch {
            chatRepository
                .create(credentials.sessionId)
                .onOk { chat ->
                    log.i { "Chat created: ${chat.id}" }
                    navigator.push(ChatScreen(credentials.sessionId, chat.id))
                }.onErr {
                    log.e { "Failed to create chat: $it" }
                    showSnackbar("Couldn't create chat", isError = true)
                }
        }
    }

    private fun openChat(chatId: ChatId) {
        val credentials = innerState.value.sessionCredentials ?: return
        navigator.push(ChatScreen(credentials.sessionId, chatId))
    }

    private fun deleteChat(chatId: ChatId) {
        val credentials = innerState.value.sessionCredentials ?: return
        viewModelScope.launch {
            chatRepository
                .delete(credentials.sessionId, chatId)
                .onOk { log.i { "Chat deleted: $chatId" } }
                .onErr {
                    log.e { "Failed to delete chat: $it" }
                    showSnackbar("Couldn't delete chat", isError = true)
                }
        }
    }

    private fun openIngredients() {
        val credentials = innerState.value.sessionCredentials ?: return
        navigator.push(IngredientsScreen(credentials.sessionId))
    }

    private fun openDebug() {
        navigator.push(DebugScreen())
    }

    private fun openRecipe(recipeId: RecipeId) {
        val credentials = innerState.value.sessionCredentials ?: return
        navigator.push(RecipeDetailScreen(credentials.sessionId, recipeId))
    }

    private fun deleteRecipe(recipeId: RecipeId) {
        val credentials = innerState.value.sessionCredentials ?: return
        viewModelScope.launch {
            recipeRepository
                .delete(credentials.sessionId, recipeId)
                .onOk { log.i { "Recipe deleted: $recipeId" } }
                .onErr {
                    log.e { "Failed to delete recipe: $it" }
                    showSnackbar("Couldn't delete recipe", isError = true)
                }
        }
    }

    private fun logOut() {
        restartChatStream(credentials = null)
        restartRecipeStream(credentials = null)
        viewModelScope.launch {
            // TODO Handle failure to log out?
            sessionRepository.logOut()
        }
        innerState.update {
            it.copy(
                sessionCredentials = null,
                chats = emptyList(),
                recipeSummaries = emptyList(),
                inputUsername = "",
                inputPassword = "",
            )
        }
    }

    private fun restartChatStream(credentials: SessionCredentials?) {
        val job = credentials?.let { creds -> viewModelScope.launch { streamChats(creds) } }
        streamChatsJob.getAndSet(job)?.cancel()
    }

    private suspend fun streamChats(credentials: SessionCredentials) {
        chatRepository
            .stream(credentials.sessionId)
            .collect { chats -> innerState.update { it.copy(chats = chats) } }
    }

    private fun restartRecipeStream(credentials: SessionCredentials?) {
        val job = credentials?.let { creds -> viewModelScope.launch { streamRecipes(creds) } }
        streamRecipesJob.getAndSet(job)?.cancel()
    }

    private suspend fun streamRecipes(credentials: SessionCredentials) {
        recipeRepository
            .streamSummaries(credentials.sessionId)
            .collect { summaries -> innerState.update { it.copy(recipeSummaries = summaries) } }
    }
}

data class State(
    val initialized: Boolean = false,
    val sessionCredentials: SessionCredentials? = null,
    val chats: List<Chat> = emptyList(),
    val recipeSummaries: List<RecipeSummary> = emptyList(),
    val inputUsername: String = "",
    val inputPassword: String = "",
    val authenticating: Boolean = false,
) {
    val inputCredentials: UserCredentials
        get() = UserCredentials(inputUsername, inputPassword)
}

data class UiState(
    val content: Content,
    val onClickDebug: () -> Unit,
) {
    sealed interface Content {
        data object Loading : Content

        data class LoggedOut(
            val username: String,
            val password: String,
            val onUsernameChange: (String) -> Unit,
            val onPasswordChange: (String) -> Unit,
            val onClickRegister: (() -> Unit)?,
            val onClickLogin: (() -> Unit)?,
        ) : Content

        data class LoggedIn(
            val username: String,
            val chats: List<UiChat>,
            val recipeSummaries: List<UiRecipeSummary>,
            val onClickNewChat: () -> Unit,
            val onClickIngredients: () -> Unit,
            val onClickLogout: () -> Unit,
        ) : Content
    }
}

data class UiChat(
    val id: ChatId,
    val title: String,
    val onClick: (ChatId) -> Unit,
    val onClickDelete: (ChatId) -> Unit,
)

data class UiRecipeSummary(
    val id: RecipeId,
    val title: String,
    val imageUrl: ImageUrl?,
    val onClickOpen: (RecipeId) -> Unit,
    val onClickDelete: (RecipeId) -> Unit,
)
