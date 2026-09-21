package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable
import kotlinx.io.files.Path

@Composable
actual fun CapturePhoto(
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    TakePhoto(
        onSuccess = { path -> onPhotos(listOf(Path(path))) },
        onCancelled = onCancelled,
        onError = onError,
    )
}
