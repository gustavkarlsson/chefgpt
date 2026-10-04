package se.gustavkarlsson.chefgpt.screens.photos

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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.camera.CapturePhoto
import se.gustavkarlsson.chefgpt.photos.PickPhotos
import kotlin.math.ceil

/** Callbacks for the camera add-tile. Null when the platform doesn't support camera capture. */
data class CameraAction(
    val onPhotoTaken: (photo: Path) -> Unit,
    val onError: () -> Unit,
)

/** Callback for the picker add-tile. Null when the platform doesn't support a file picker. */
data class PickerAction(
    val onPhotosPicked: (photos: List<Path>) -> Unit,
    val onError: () -> Unit,
)

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

/**
 * The shared "collect photos" UI: thumbnails of the collected photos, one add-tile per
 * supported source (camera and/or file picker), and a confirm button. Used by the chat
 * add-photos sheet and the recipe scan sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoCollectionSheetContent(
    photos: List<Path>,
    camera: CameraAction?,
    picker: PickerAction?,
    onClickPhoto: (Path) -> Unit,
    onClickConfirm: (() -> Unit)?,
    confirmLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = computeTileSize(maxWidth)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(TILE_SPACING),
                verticalArrangement = Arrangement.spacedBy(TILE_SPACING),
            ) {
                photos.forEach { photo ->
                    PhotoTile(
                        photo = photo,
                        onClick = { onClickPhoto(photo) },
                        modifier = Modifier.size(tileSize),
                    )
                }
                if (camera != null) {
                    CameraAddTile(action = camera, modifier = Modifier.size(tileSize))
                }
                if (picker != null) {
                    PickerAddTile(action = picker, modifier = Modifier.size(tileSize))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onClickConfirm?.invoke() },
            enabled = onClickConfirm != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(confirmLabel)
        }
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
private fun CameraAddTile(
    action: CameraAction,
    modifier: Modifier = Modifier,
) {
    var capturing by rememberSaveable { mutableStateOf(false) }
    AddTile(
        icon = Icons.Default.CameraAlt,
        contentDescription = "Take photo",
        onClick = { capturing = true },
        enabled = !capturing,
        modifier = modifier,
    )
    if (capturing) {
        CapturePhoto(
            onPhoto = { photo ->
                action.onPhotoTaken(photo)
                capturing = false
            },
            onCancelled = {
                capturing = false
            },
            onError = {
                action.onError()
                capturing = false
            },
        )
    }
}

@Composable
private fun PickerAddTile(
    action: PickerAction,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    AddTile(
        icon = Icons.Default.PhotoLibrary,
        contentDescription = "Choose photos",
        onClick = { picking = true },
        enabled = !picking,
        modifier = modifier,
    )
    if (picking) {
        PickPhotos(
            multiple = true,
            onPhotos = { photos ->
                action.onPhotosPicked(photos)
                picking = false
            },
            onCancelled = {
                picking = false
            },
            onError = {
                action.onError()
                picking = false
            },
        )
    }
}

@Composable
private fun AddTile(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick, enabled = enabled),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
