package se.gustavkarlsson.chefgpt.screens.start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val modifier = Modifier.fillMaxSize().padding(innerPadding)
        when (uiState) {
            // Blank screen until we know whether a session exists
            UiState.Loading -> Unit

            is UiState.LoggedOut -> LoggedOutContent(modifier = modifier, state = uiState)

            is UiState.LoggedIn -> LoggedInContent(modifier = modifier, state = uiState)
        }
    }
}

@Composable
private fun LoggedOutContent(
    state: UiState.LoggedOut,
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

@Composable
private fun LoggedInContent(
    state: UiState.LoggedIn,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        ChatSidebar(
            chats = state.chats,
            modifier = Modifier.width(260.dp).fillMaxHeight(),
        )
        VerticalDivider()
        WelcomePanel(
            username = state.username,
            onClickNewChat = state.onClickNewChat,
            onClickIngredients = state.onClickIngredients,
            onClickLogout = state.onClickLogout,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun WelcomePanel(
    username: String,
    onClickNewChat: () -> Unit,
    onClickIngredients: () -> Unit,
    onClickLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome back, $username!",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Ready to find your next recipe?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onClickNewChat) {
            Text("New chat")
        }
        OutlinedButton(
            onClick = onClickIngredients,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("My ingredients")
        }
        OutlinedButton(
            onClick = onClickLogout,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Log out")
        }
    }
}

@Composable
private fun ChatSidebar(
    chats: List<UiChat>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
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
