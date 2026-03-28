package se.gustavkarlsson.chefgpt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed interface StartScreenState {
    data class LoggedOut(
        val onClickLogin: () -> Unit,
    ) : StartScreenState

    data class LoggedIn(
        val userName: String,
        val onClickStartChatting: () -> Unit,
    ) : StartScreenState
}

@Composable
fun StartScreen(
    state: StartScreenState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            is StartScreenState.LoggedOut -> {
                LoggedOutContent(onClickLogin = state.onClickLogin)
            }

            is StartScreenState.LoggedIn -> {
                LoggedInContent(
                    userName = state.userName,
                    onClickStartChatting = state.onClickStartChatting,
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
