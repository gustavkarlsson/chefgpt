package se.gustavkarlsson.chefgpt.screens.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id

@Serializable
@SerialName("loading")
data class LoadingScreen(
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<LoadingViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState)
    }
}

@Composable
private fun Content(
    uiState: UiState,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                UiState.Loading -> {
                    CircularProgressIndicator()
                }

                is UiState.Failed -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Couldn't reach the server",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = uiState.onClickRetry) {
                            Text("Retry")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = uiState.onClickLogout) {
                            Text("Log out")
                        }
                    }
                }
            }
        }
    }
}
