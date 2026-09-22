package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import se.gustavkarlsson.chefgpt.IoOrDefault
import se.gustavkarlsson.chefgpt.pickFiles

@Composable
actual fun CapturePhoto(
    onPhoto: (photo: Path) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    LaunchedEffect(Unit) {
        val picked = pickFiles(multiple = false).firstOrNull()
        if (picked == null) {
            onCancelled()
            return@LaunchedEffect
        }
        onPhoto(copyIntoPhotoCache(picked))
    }
}

// The picker returns the user's own file; copy it into a cache dir so the caller can
// delete it without touching the original.
private suspend fun copyIntoPhotoCache(file: Path): Path =
    withContext(Dispatchers.IoOrDefault) {
        val dir = Path("${System.getProperty("java.io.tmpdir")}/chefgpt/photos")
        SystemFileSystem.createDirectories(dir)
        val target = Path("$dir/${file.name}")
        SystemFileSystem.sink(target).buffered().use { sink ->
            SystemFileSystem.source(file).buffered().use { source ->
                source.transferTo(sink)
            }
        }
        target
    }
