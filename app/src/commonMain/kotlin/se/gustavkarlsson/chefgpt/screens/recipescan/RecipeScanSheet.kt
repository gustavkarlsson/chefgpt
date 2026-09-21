package se.gustavkarlsson.chefgpt.screens.recipescan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.camera.CapturePhoto
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id
import se.gustavkarlsson.chefgpt.sessions.SessionId

@Serializable
@SerialName("recipe-scan-sheet")
data class RecipeScanSheet(
    val sessionId: SessionId,
    override val id: Id = Id.new(),
) : Screen.BottomSheet {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<RecipeScanSheetViewModel> { parametersOf(this) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Content(
    uiState: RecipeScanSheetUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = "Scan recipes", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Take photos of one or more recipes. I'll save each one I find.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.photos.forEach { photo ->
                PhotoTile(photo = photo, onClick = { uiState.onClickPhoto(photo) })
            }
            AddPhotoTile(onClick = uiState.onClickAddPhoto)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { uiState.onClickConfirm?.invoke() },
            enabled = uiState.onClickConfirm != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Confirm")
        }
    }

    if (uiState.capturing) {
        CapturePhoto(
            onPhotos = uiState.onPhotosCaptured,
            onCancelled = uiState.onCaptureCancelled,
            onError = uiState.onCaptureError,
        )
    }

    if (uiState.discardTarget != null) {
        AlertDialog(
            onDismissRequest = uiState.onDiscardDismissed,
            title = { Text("Discard photo?") },
            text = { Text("This photo won't be added to the scan.") },
            confirmButton = {
                TextButton(onClick = uiState.onDiscardConfirmed) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = uiState.onDiscardDismissed) { Text("Keep") }
            },
        )
    }
}

@Composable
private fun PhotoTile(
    photo: Path,
    onClick: () -> Unit,
) {
    AsyncImage(
        model = photo,
        contentDescription = "Photo",
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick),
    )
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(72.dp).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Add photo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
