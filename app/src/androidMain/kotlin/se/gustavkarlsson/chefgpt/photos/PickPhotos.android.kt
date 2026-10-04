package se.gustavkarlsson.chefgpt.photos

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.ChefGptApplication
import java.io.File

@Composable
actual fun PickPhotos(
    multiple: Boolean,
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var launched by rememberSaveable { mutableStateOf(false) }

    val pickSingle =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            if (uri == null) {
                onCancelled()
            } else {
                scope.launch {
                    val photo = withContext(Dispatchers.IO) { copyToPhotoCache(uri) }
                    if (photo == null) onError() else onPhotos(listOf(photo))
                }
            }
        }
    val pickMultiple =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) { uris ->
            if (uris.isEmpty()) {
                onCancelled()
            } else {
                scope.launch {
                    val photos = withContext(Dispatchers.IO) { uris.mapNotNull(::copyToPhotoCache) }
                    if (photos.isEmpty()) onError() else onPhotos(photos)
                }
            }
        }
    LaunchedEffect(Unit) {
        if (launched) return@LaunchedEffect // Prevents re-launching on recomposition/rotation
        launched = true
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (multiple) {
            pickMultiple.launch(request)
        } else {
            pickSingle.launch(request)
        }
    }
}

// Copies a picked image into the app's photo cache. The picker hands out Uris to
// the user's own files, so callers that delete their copies must not get the originals.
private fun copyToPhotoCache(uri: Uri): Path? {
    val context = ChefGptApplication.context
    val photosCache = context.cacheDir.resolve("photos")
    if (!photosCache.exists() && !photosCache.mkdirs()) return null
    val mimeType = context.contentResolver.getType(uri)
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
    val file = File.createTempFile("photo-", ".$extension", photosCache)
    return context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
        Path(file.absolutePath)
    }
}
