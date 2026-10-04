package se.gustavkarlsson.chefgpt.photos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.io.files.Path

@Composable
actual fun PickPhotos(
    multiple: Boolean,
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    // TODO Implement a file picker
    LaunchedEffect(Unit) { onError() }
}
