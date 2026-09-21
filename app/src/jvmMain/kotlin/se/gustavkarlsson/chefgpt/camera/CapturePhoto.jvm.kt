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
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    LaunchedEffect(Unit) {
        val picked = pickFiles(multiple = true)
        if (picked.isEmpty()) {
            onCancelled()
            return@LaunchedEffect
        }
        onPhotos(copyIntoPhotoCache(picked))
    }
}

// The picker returns the user's own files; copy them into a cache dir so the caller can
// delete them without touching the originals.
private suspend fun copyIntoPhotoCache(files: List<Path>): List<Path> =
    withContext(Dispatchers.IoOrDefault) {
        val dir = Path("${System.getProperty("java.io.tmpdir")}/chefgpt/photos")
        SystemFileSystem.createDirectories(dir)
        files.map { file ->
            val target = Path("$dir/${file.name}")
            SystemFileSystem.sink(target).buffered().use { sink ->
                SystemFileSystem.source(file).buffered().use { source ->
                    source.transferTo(sink)
                }
            }
            target
        }
    }
