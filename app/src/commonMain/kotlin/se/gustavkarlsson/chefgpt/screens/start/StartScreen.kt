package se.gustavkarlsson.chefgpt.screens.start

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id
import se.gustavkarlsson.chefgpt.plus
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessage
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessageHost
import se.gustavkarlsson.chefgpt.snackbar.rememberSnackbarHostState

@Serializable
@SerialName("start")
data class StartScreen(
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<StartViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState, viewModel.snackbarMessages)
    }
}

@Composable
private fun Content(
    uiState: UiState,
    snackbarMessages: Flow<SnackbarMessage>,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessages)
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = modifier.fillMaxSize()) {
            when (val content = uiState.content) {
                // Blank screen until we know whether a session exists
                UiState.Content.Loading -> Unit

                is UiState.Content.LoggedOut -> LoggedOutContent(state = content)

                is UiState.Content.LoggedIn -> LoggedInContent(state = content)
            }
            // Always-available entry point to the debug screen.
            IconButton(
                onClick = uiState.onClickDebug,
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Debug",
                )
            }
            SnackbarMessageHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding(),
            )
        }
    }
}

@Composable
private fun LoggedOutContent(
    state: UiState.Content.LoggedOut,
    modifier: Modifier = Modifier,
) {
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { usernameFocusRequester.requestFocus() }
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime),
        contentPadding = WindowInsets.safeDrawing.exclude(WindowInsets.ime).asPaddingValues() + PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item {
            Text(
                text = "Welcome to ChefGPT",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                text = "Sign in to get started",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                value = state.username,
                onValueChange = state.onUsernameChange,
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                modifier = Modifier.focusRequester(usernameFocusRequester),
            )
        }
        item {
            var showPassword by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = state.password,
                onValueChange = state.onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation =
                    if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector =
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                modifier =
                    Modifier
                        .padding(top = 8.dp, bottom = 16.dp)
                        .focusRequester(passwordFocusRequester),
            )
        }
        item {
            Button(onClick = { state.onClickRegister?.invoke() }, enabled = state.onClickRegister != null) {
                Text("Register")
            }
        }
        item {
            Button(onClick = { state.onClickLogin?.invoke() }, enabled = state.onClickLogin != null) {
                Text("Sign in")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun LoggedInContent(
    state: UiState.Content.LoggedIn,
    modifier: Modifier = Modifier,
) {
    val directive =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).let { base ->
            // Allow all three panes to be visible simultaneously on wide screens.
            // Extra (recipes) collapses before List (chats) by default adapt-strategy priority.
            if (base.maxHorizontalPartitions >= 2) {
                base.copy(maxHorizontalPartitions = 3)
            } else {
                base
            }
        }
    val navigator =
        rememberListDetailPaneScaffoldNavigator<Nothing>(
            scaffoldDirective = directive,
            initialDestinationHistory =
                listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail)),
        )
    val scope = rememberCoroutineScope()
    val listHidden =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    val extraHidden =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.Extra] == PaneAdaptedValue.Hidden
    @Suppress("DEPRECATION")
    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }
    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                ChatSidebar(
                    modifier = Modifier.fillMaxSize(),
                    chats = state.chats,
                    onClickBack =
                        if (navigator.canNavigateBack()) {
                            { scope.launch { navigator.navigateBack() } }
                        } else {
                            null
                        },
                )
            }
        },
        extraPane = {
            AnimatedPane {
                RecipeSidebar(
                    modifier = Modifier.fillMaxSize(),
                    recipes = state.recipeSummaries,
                    onClickBack =
                        if (navigator.canNavigateBack()) {
                            { scope.launch { navigator.navigateBack() } }
                        } else {
                            null
                        },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                WelcomePanel(
                    modifier = Modifier.fillMaxSize(),
                    username = state.username,
                    onClickNewChat = state.onClickNewChat,
                    onClickIngredients = state.onClickIngredients,
                    onClickLogout = state.onClickLogout,
                    onClickViewChats =
                        if (listHidden) {
                            { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.List) } }
                        } else {
                            null
                        },
                    onClickViewRecipes =
                        if (extraHidden) {
                            { scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Extra) } }
                        } else {
                            null
                        },
                )
            }
        },
    )
}

@Composable
private fun WelcomePanel(
    username: String,
    onClickNewChat: () -> Unit,
    onClickIngredients: () -> Unit,
    onClickLogout: () -> Unit,
    modifier: Modifier = Modifier,
    onClickViewChats: (() -> Unit)? = null,
    onClickViewRecipes: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime),
        contentPadding = WindowInsets.safeDrawing.exclude(WindowInsets.ime).asPaddingValues() + PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = CenterVerticallyWithLastItemAtEnd,
    ) {
        item {
            Text(
                text = "Welcome back\n$username!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
        }
        item {
            Text(
                text = "Ready to find your next recipe?",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Spacer(Modifier.height(24.dp))
        }
        item {
            Button(onClick = onClickNewChat) {
                Text("New chat")
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
        }
        item {
            AnimatedVisibility(
                visible = onClickViewChats != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically),
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(vertical = 4.dp),
                    onClick = { onClickViewChats?.invoke() },
                ) {
                    Text("Previous chats")
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
        }
        item {
            AnimatedVisibility(
                visible = onClickViewRecipes != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically),
            ) {
                OutlinedButton(
                    modifier = Modifier.padding(vertical = 4.dp),
                    onClick = { onClickViewRecipes?.invoke() },
                ) {
                    Text("Recipes")
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
        }
        item {
            OutlinedButton(
                onClick = onClickIngredients,
            ) {
                Text("Ingredients")
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
        }

        // Bottom item
        item {
            TextButton(onClick = onClickLogout) {
                Text("Log out")
            }
        }
    }
}

private data object CenterVerticallyWithLastItemAtEnd : Arrangement.Vertical {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) {
        if (sizes.isEmpty()) return

        val lastSize = sizes.last() // Size of the last (ending) item
        val availableSpaceForRest = totalSize - lastSize // Space available for the rest of the items
        val sizeOfRest = sizes.sum() - lastSize // Size of the rest of the items combined
        val freeSpace = (availableSpaceForRest - sizeOfRest).coerceAtLeast(0)
        val centerPadding = freeSpace / 2

        var offset = centerPadding
        sizes.dropLast(1).forEachIndexed { index, size ->
            outPositions[index] = offset
            offset += size
        }
        offset += centerPadding
        outPositions[outPositions.lastIndex] = offset
    }
}

@Composable
private fun ChatSidebar(
    chats: List<UiChat>,
    modifier: Modifier = Modifier,
    onClickBack: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                        ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onClickBack != null) {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
                Text(
                    text = "Previous chats",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }
            HorizontalDivider()
            if (chats.isEmpty()) {
                Text(
                    text = "No chats yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom),
                            ).padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding =
                        WindowInsets.safeDrawing
                            .exclude(WindowInsets.ime)
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                ) {
                    items(chats, key = { it.id.toString() }) { chat ->
                        ChatItem(
                            chat = chat,
                            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Start).asPaddingValues(),
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatItem(
    chat: UiChat,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { chat.onClick(chat.id) }
                .padding(contentPadding)
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = chat.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { chat.onClickDelete(chat.id) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete chat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecipeSidebar(
    recipes: List<UiRecipeSummary>,
    modifier: Modifier = Modifier,
    onClickBack: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                        ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onClickBack != null) {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }
                Text(
                    text = "Recipes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }
            HorizontalDivider()
            if (recipes.isEmpty()) {
                Text(
                    text = "No saved recipes yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom),
                            ).padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding =
                        WindowInsets.safeDrawing
                            .exclude(WindowInsets.ime)
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                ) {
                    items(recipes, key = { it.id.toString() }) { recipe ->
                        RecipeItem(
                            recipe = recipe,
                            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Start).asPaddingValues(),
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeItem(
    recipe: UiRecipeSummary,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            if (recipe.imageUrl != null) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Surface(
                    modifier = Modifier.matchParentSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { recipe.onClickDelete(recipe.id) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete recipe",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
