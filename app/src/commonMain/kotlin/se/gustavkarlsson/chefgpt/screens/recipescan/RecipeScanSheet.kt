package se.gustavkarlsson.chefgpt.screens.recipescan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.Dp
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
import kotlin.math.ceil

private val TILE_SPACING = 8.dp
private val MIN_TILE_SIZE = 64.dp
private val MAX_TILE_SIZE = 96.dp

private fun computeTileSize(availableWidth: Dp): Dp {
    val spacing = TILE_SPACING.value
    val width = availableWidth.value
    val maxSize = MAX_TILE_SIZE.value
    val columns = ceil((width + spacing) / (maxSize + spacing)).toInt().coerceAtLeast(1)
    return Dp((width + spacing) / columns - spacing).coerceIn(MIN_TILE_SIZE, MAX_TILE_SIZE)
}

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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = computeTileSize(maxWidth)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TILE_SPACING),
                verticalArrangement = Arrangement.spacedBy(TILE_SPACING),
            ) {
                uiState.photos.forEach { photo ->
                    PhotoTile(
                        photo = photo,
                        onClick = { uiState.onClickPhoto(photo) },
                        modifier = Modifier.size(tileSize),
                    )
                }
                AddPhotoTile(
                    onClick = uiState.onClickAddPhoto,
                    modifier = Modifier.size(tileSize),
                )
            }
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
            onPhoto = uiState.onPhotoCaptured,
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
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = photo,
        contentDescription = "Photo",
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick),
    )
}

@Composable
private fun AddPhotoTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
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
