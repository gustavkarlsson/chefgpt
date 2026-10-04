package se.gustavkarlsson.chefgpt.photos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import se.gustavkarlsson.chefgpt.isImageFile
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun PickPhotos(
    multiple: Boolean,
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    LaunchedEffect(Unit) {
        val picked =
            withContext(Dispatchers.Default) {
                val fileDialog = FileDialog(null as Frame?, "Select Images", FileDialog.LOAD)
                fileDialog.isMultipleMode = multiple
                fileDialog.setFilenameFilter { _, name -> isImageFile(name) }
                fileDialog.isVisible = true // Blocks
                fileDialog.files.map { copyIntoPhotoCache(Path(it.absolutePath)) }
            }
        if (picked.isEmpty()) {
            onCancelled()
        } else {
            onPhotos(picked)
        }
    }
}

// The picker returns the user's own files; copy each into a cache dir so callers
// can delete their copies without touching the originals.
private fun copyIntoPhotoCache(file: Path): Path {
    val cacheDir = File("${System.getProperty("java.io.tmpdir")}/chefgpt/photos")
    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
        error("Couldn't create photo cache directory")
    }
    val suffix =
        file.name
            .substringAfterLast('.', "")
            .takeIf { it.isNotEmpty() }
            ?.let { ".$it" }
            .orEmpty()
    val target = File.createTempFile("photo-", suffix, cacheDir)
    SystemFileSystem.sink(Path(target.absolutePath)).buffered().use { sink ->
        SystemFileSystem.source(file).buffered().use { source ->
            source.transferTo(sink)
        }
    }
    return Path(target.absolutePath)
}
