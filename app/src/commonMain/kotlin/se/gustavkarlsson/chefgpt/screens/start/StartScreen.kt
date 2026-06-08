package se.gustavkarlsson.chefgpt.screens.start

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartScreen() {
    val viewModel = koinViewModel<StartViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Content(uiState)
}

@Composable
private fun Content(
    uiState: UiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val content = uiState.content) {
                // Blank screen until we know whether a session exists
                UiState.Content.Loading -> Unit

                is UiState.Content.LoggedOut -> LoggedOutContent(modifier = Modifier.fillMaxSize(), state = content)

                is UiState.Content.LoggedIn -> LoggedInContent(modifier = Modifier.fillMaxSize(), state = content)
            }
            // Always-available entry point to the debug screen.
            IconButton(
                onClick = uiState.onClickDebug,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Debug",
                )
            }
        }
    }
}

@Composable
private fun LoggedOutContent(
    state: UiState.Content.LoggedOut,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome to ChefGPT",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Sign in to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        val usernameFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { usernameFocusRequester.requestFocus() }
        OutlinedTextField(
            value = state.username,
            onValueChange = state.onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.focusRequester(usernameFocusRequester),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = { state.onClickRegister?.invoke() }, enabled = state.onClickRegister != null) {
            Text("Register")
        }
        Button(onClick = { state.onClickLogin?.invoke() }, enabled = state.onClickLogin != null) {
            Text("Sign in")
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun LoggedInContent(
    state: UiState.Content.LoggedIn,
    modifier: Modifier = Modifier,
) {
    val navigator =
        rememberListDetailPaneScaffoldNavigator<Nothing>(
            initialDestinationHistory =
                listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail)),
        )
    val scope = rememberCoroutineScope()
    val listHidden =
        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
    // Drive the scaffold's predictive back animation from the system back gesture, finalizing the
    // navigation when committed and rewinding when cancelled. Also handles plain back presses.
    @Suppress("DEPRECATION")
    PredictiveBackHandler(enabled = navigator.canNavigateBack()) { progress ->
        try {
            progress.collect { backEvent -> navigator.seekBack(fraction = backEvent.progress) }
            navigator.navigateBack()
        } catch (_: CancellationException) {
            navigator.seekBack(fraction = 0f)
        }
    }
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                ChatSidebar(
                    chats = state.chats,
                    onClickBack =
                        if (navigator.canNavigateBack()) {
                            { scope.launch { navigator.navigateBack() } }
                        } else {
                            null
                        },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        detailPane = {
            AnimatedPane {
                WelcomePanel(
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
                    modifier = Modifier.fillMaxSize(),
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
) {
    BoxWithConstraints(modifier = modifier.padding(16.dp)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "Welcome back\n$username!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Ready to find your next recipe?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Button(onClick = onClickNewChat) {
                Text("New chat")
            }
            AnimatedVisibility(visible = onClickViewChats != null) {
                OutlinedButton(
                    onClick = { onClickViewChats?.invoke() },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Previous chats")
                }
            }
            OutlinedButton(
                onClick = onClickIngredients,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Ingredients")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClickLogout) {
                Text("Log out")
            }
        }
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
                modifier = Modifier.fillMaxWidth().padding(8.dp),
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
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(chats, key = { it.id.toString() }) { chat ->
                        ChatItem(
                            chat = chat,
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { chat.onClick(chat.id) }
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
