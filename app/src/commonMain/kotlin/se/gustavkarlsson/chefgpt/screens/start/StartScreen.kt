package se.gustavkarlsson.chefgpt.screens.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import se.gustavkarlsson.chefgpt.LocalNavigator
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel.ViewState

@Composable
fun StartScreen() {
    val viewModel = koinViewModel<StartViewModel>()
    val viewState by viewModel.viewState.collectAsState()
    Content(viewState)
}

@Composable
private fun Content(viewState: ViewState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (viewState) {
            is ViewState.LoggedOut -> {
                LoggedOutContent(
                    username = viewState.username,
                    password = viewState.password,
                    onUsernameChange = viewState.onUsernameChange,
                    onPasswordChange = viewState.onPasswordChange,
                    onClickRegister = viewState.onClickRegister,
                    onClickLogin = viewState.onClickLogin,
                )
            }

            is ViewState.LoggedIn -> {
                val navigator = LocalNavigator.current
                LoggedInContent(
                    username = viewState.username,
                    onClickStartChatting = { viewState.onClickStartChatting(navigator) },
                )
            }
        }
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
    onClickStartChatting: () -> Unit,
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
    Button(onClick = onClickStartChatting) {
        Text("Start chatting")
    }
}
