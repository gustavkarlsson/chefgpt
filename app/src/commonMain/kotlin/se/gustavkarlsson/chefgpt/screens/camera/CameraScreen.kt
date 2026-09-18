package se.gustavkarlsson.chefgpt.screens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
    val modifier = Modifier.fillMaxSize()
    when (uiState) {
        is UiState.UsingCamera -> {
            UsingCamera(modifier = modifier, uiState = uiState)
        }

        is UiState.DisplayingPhoto -> {
            DisplayingPhoto(modifier = modifier, uiState = uiState)
        }

        is UiState.PermissionDenied -> {
            PermissionDenied(
                modifier = modifier,
                uiState = uiState,
            )
        }
    }
}

@Composable
private fun DisplayingPhoto(
    uiState: UiState.DisplayingPhoto,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = uiState.photoPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        CloseButton(onClick = uiState.onClickClose, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun UsingCamera(
    uiState: UiState.UsingCamera,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CameraView(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            onPhotoTaken = uiState.onPhotoTaken,
            onPermissionDenied = uiState.onPermissionDenied,
            onError = uiState.onError,
        )
        CloseButton(onClick = uiState.onClickClose, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
expect fun CameraView(
    onPhotoTaken: (Path) -> Unit,
    onPermissionDenied: (fixable: Boolean) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
private fun PermissionDenied(
    uiState: UiState.PermissionDenied,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CloseButton(onClick = uiState.onClickClose, modifier = Modifier.align(Alignment.TopEnd))
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Camera permission is needed to take a photo", textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            if (uiState.onClickOpenPermissions != null) {
                TextButton(onClick = uiState.onClickOpenPermissions) {
                    Text("Open permissions")
                }
            }
        }
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null, // TODO Content description
        )
    }
}
