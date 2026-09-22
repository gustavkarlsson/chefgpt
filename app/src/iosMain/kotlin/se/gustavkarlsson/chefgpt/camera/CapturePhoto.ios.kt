package se.gustavkarlsson.chefgpt.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import se.gustavkarlsson.chefgpt.IoOrDefault

private const val JPEG_QUALITY = 0.9

@Composable
actual fun CapturePhoto(
    onPhotos: (photos: List<Path>) -> Unit,
    onCancelled: () -> Unit,
    onError: () -> Unit,
) {
    val hostController = LocalUIViewController.current
    val scope = rememberCoroutineScope()
    val currentOnPhotos by rememberUpdatedState(onPhotos)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnError by rememberUpdatedState(onError)

    // Remembered because UIImagePickerController holds its delegate weakly.
    val pickerDelegate =
        remember {
            PhotoPickerDelegate(
                onPicked = { image ->
                    scope.launch {
                        val path = image?.let { withContext(Dispatchers.IoOrDefault) { writeToPhotoCache(it) } }
                        if (path != null) {
                            currentOnPhotos(listOf(Path(path)))
                        } else {
                            currentOnError()
                        }
                    }
                },
                onCancelled = { currentOnCancelled() },
            )
        }

    LaunchedEffect(Unit) {
        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypeCamera)) {
            currentOnError()
            return@LaunchedEffect
        }
        val picker =
            UIImagePickerController().apply {
                sourceType = UIImagePickerControllerSourceTypeCamera
                delegate = pickerDelegate
            }
        hostController.presentViewController(picker, animated = true, completion = null)
    }
}

private class PhotoPickerDelegate(
    private val onPicked: (UIImage?) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true) { onPicked(image) }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) { onCancelled() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun writeToPhotoCache(image: UIImage): String? {
    val data = UIImageJPEGRepresentation(image, JPEG_QUALITY) ?: return null
    val caches =
        NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: return null
    val directory = "$caches/photos"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = directory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    val path = "$directory/photo-${NSUUID().UUIDString}.jpg"
    return if (data.writeToFile(path, atomically = true)) path else null
}
