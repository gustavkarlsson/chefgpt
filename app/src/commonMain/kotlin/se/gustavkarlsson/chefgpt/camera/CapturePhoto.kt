package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable
import kotlinx.io.files.Path

/**
 * Lets the user capture a photo. The returned file is an app-owned copy in a cache dir,
 * safe to delete once the caller is done with it. Launches the platform capture as soon
 * as it enters composition.
 */
@Composable
expect fun CapturePhoto(
    onPhoto: (photo: Path) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
)
