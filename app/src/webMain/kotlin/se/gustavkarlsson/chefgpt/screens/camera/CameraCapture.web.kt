package se.gustavkarlsson.chefgpt.screens.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path

// No camera on this platform; leave immediately so the button is effectively a no-op.
@Composable
actual fun CameraCapture(
    onPhotoCaptured: (Path) -> Unit,
    onPermissionDenied: () -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) { onCancelled() }
}
