package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.io.files.Path

@Composable
actual fun CapturePhoto(
    onPhoto: (photo: Path) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    // TODO Implement capture
    LaunchedEffect(Unit) { onError() }
}
