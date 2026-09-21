package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable
import kotlinx.io.files.Path

/**
 * Lets the user capture one or more photos. The returned files are app-owned copies in
 * a cache dir, safe to delete once the caller is done with them. Launches the platform
 * capture as soon as it enters composition.
 */
@Composable
expect fun CapturePhoto(
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
)
