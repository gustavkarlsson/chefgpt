package se.gustavkarlsson.chefgpt.screens.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path

@Composable
actual fun CameraView(
    onPhotoTaken: (Path) -> Unit,
    onPermissionDenied: (fixable: Boolean) -> Unit,
    onError: () -> Unit,
    modifier: Modifier,
) {
    // TODO Implement camera
}
