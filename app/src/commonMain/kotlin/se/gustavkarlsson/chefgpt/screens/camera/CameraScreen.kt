package se.gustavkarlsson.chefgpt.screens.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id

@Serializable
@SerialName("camera")
data class CameraScreen(
    override val id: Id = Id.new(),
) : Screen.ResultProvider<CapturedPhoto> {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<CameraViewModel> { parametersOf(this) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState)
    }
}

@Composable
private fun Content(uiState: UiState) {
    if (uiState.permissionDenied) {
        PermissionDenied(onBack = uiState.onCancelled)
    } else {
        CameraCapture(
            onPhotoCaptured = uiState.onPhotoCaptured,
            onPermissionDenied = uiState.onPermissionDenied,
            onCancelled = uiState.onCancelled,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PermissionDenied(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera permission is needed to take a photo")
        TextButton(
            modifier = Modifier.padding(top = 16.dp),
            onClick = onBack,
        ) {
            Text("Back")
        }
    }
}

@Composable
expect fun CameraCapture(
    onPhotoCaptured: (Path) -> Unit,
    onPermissionDenied: () -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier,
)
