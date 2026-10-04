package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.photos.PickPhotos

// Unreachable on desktop (supportsCamera is false), but the common camera tile
// still requires an actual to compile. Picking a single photo is the closest thing.
@Composable
actual fun CapturePhoto(
    onPhoto: (photo: Path) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    PickPhotos(
        multiple = false,
        onPhotos = { photos ->
            val photo = photos.firstOrNull()
            if (photo == null) {
                onCancelled()
            } else {
                onPhoto(photo)
            }
        },
        onCancelled = onCancelled,
        onError = onError,
    )
}
