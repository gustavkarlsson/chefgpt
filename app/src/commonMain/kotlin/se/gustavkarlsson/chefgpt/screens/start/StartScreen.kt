package se.gustavkarlsson.chefgpt.screens.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
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
                LoggedOutContent(onClickLogin = viewState.onClickLogin)
            }

            is ViewState.LoggedIn -> {
                LoggedInContent(
                    userName = viewState.userName,
                    onClickStartChatting = viewState.onClickStartChatting,
                )
            }
        }
    }
}

@Composable
private fun LoggedOutContent(onClickLogin: () -> Unit) {
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
    Button(onClick = onClickLogin) {
        Text("Sign in")
    }
}

@Composable
private fun LoggedInContent(
    userName: String,
    onClickStartChatting: () -> Unit,
) {
    Text(
        text = "Welcome back, $userName",
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
