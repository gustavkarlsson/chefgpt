package se.gustavkarlsson.chefgpt.camera

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import java.io.File

private const val TAG = "CameraCapture"

@Composable
actual fun CapturePhoto(
    onPhoto: (photo: Path) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    var filePath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { isSuccess ->
            val path = filePath
            when {
                path == null -> {
                    Log.e(TAG, "Path missing after photo result")
                    onError()
                }

                !isSuccess -> {
                    // Probably just canceled
                    filePath = null
                    File(path).delete()
                    onCancelled()
                }

                else -> {
                    filePath = null
                    onPhoto(Path(path))
                }
            }
        }
    LaunchedEffect(Unit) {
        if (filePath != null) return@LaunchedEffect // Prevents re-launching on recomposition/rotation

        val path =
            withContext(Dispatchers.IO) {
                val photosCache = context.cacheDir.resolve("photos")
                if (photosCache.exists() || photosCache.mkdirs()) {
                    val file = File.createTempFile("photo-", ".jpg", photosCache)
                    file.absolutePath
                } else {
                    null
                }
            }
        if (path != null) {
            filePath = path
            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(path),
                )
            cameraLauncher.launch(uri)
        } else {
            onError()
        }
    }
}
