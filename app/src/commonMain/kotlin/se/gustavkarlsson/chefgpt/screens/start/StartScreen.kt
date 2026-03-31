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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import se.gustavkarlsson.chefgpt.StoredChat
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel.ViewState

@Composable
fun StartScreen() {
    val viewModel = koinViewModel<StartViewModel>()
    val viewState by viewModel.viewState.collectAsState()
    LaunchedEffect(viewState) {
        if (viewState is ViewState.LoggedIn) {
            viewModel.refreshChats()
        }
    }
    Content(viewState)
}

@Composable
private fun Content(viewState: ViewState) {
    Scaffold { innerPadding ->
        when (viewState) {
            is ViewState.LoggedOut -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    LoggedOutContent(
                        username = viewState.username,
                        password = viewState.password,
                        onUsernameChange = viewState.onUsernameChange,
                        onPasswordChange = viewState.onPasswordChange,
                        onClickRegister = viewState.onClickRegister,
                        onClickLogin = viewState.onClickLogin,
                    )
                }
            }

            is ViewState.LoggedIn -> {
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    ChatSidebar(
                        chats = viewState.chats,
                        onChatClick = viewState.onClickChat,
                        modifier = Modifier.width(260.dp).fillMaxHeight(),
                    )
                    VerticalDivider()
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LoggedInContent(
                            username = viewState.username,
                            onClickNewChat = viewState.onClickNewChat,
                            onClickLogout = viewState.onClickLogout,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSidebar(
    chats: List<StoredChat>,
    onChatClick: (StoredChat) -> Unit,
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
                            onClick = { onChatClick(chat) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatItem(
    chat: StoredChat,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
    ) {
        val formatted =
            chat.createdAt
                .toString()
                .replace("T", " ")
                .take(19)
        Text(
            text = "Chat from $formatted",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoggedOutContent(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onClickRegister: (() -> Unit)?,
    onClickLogin: (() -> Unit)?,
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
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Username") },
        singleLine = true,
        modifier = Modifier.focusRequester(usernameFocusRequester),
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
    )
    Button(onClick = { onClickRegister?.invoke() }, enabled = onClickRegister != null) {
        Text("Register")
    }
    Button(onClick = { onClickLogin?.invoke() }, enabled = onClickLogin != null) {
        Text("Sign in")
    }
}

@Composable
private fun LoggedInContent(
    username: String,
    onClickNewChat: () -> Unit,
    onClickLogout: () -> Unit,
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
        onClick = onClickLogout,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text("Log out")
    }
}
