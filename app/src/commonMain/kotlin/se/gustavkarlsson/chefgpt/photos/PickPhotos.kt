package se.gustavkarlsson.chefgpt.photos

import androidx.compose.runtime.Composable
import kotlinx.io.files.Path

/**
 * Lets the user pick image files. The returned files are app-owned copies in a
 * cache dir, safe to delete once the caller is done with them. Launches the
 * platform picker as soon as it enters composition.
 */
@Composable
expect fun PickPhotos(
    multiple: Boolean,
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
)
