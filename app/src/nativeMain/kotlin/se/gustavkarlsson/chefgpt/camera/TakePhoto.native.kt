package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable

@Composable
actual fun TakePhoto(
    onSuccess: (String) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    // TODO Implement camera
}
