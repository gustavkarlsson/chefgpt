package se.gustavkarlsson.chefgpt.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.io.files.Path
import java.io.File

private const val TAG = "CameraCapture"

@Composable
actual fun CameraView(
    onPhotoTaken: (Path) -> Unit,
    onPermissionDenied: (fixable: Boolean) -> Unit,
    onError: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            if (!granted) {
                // TODO Add second try if possible
                onPermissionDenied(true) // Android permission errors are fixable
            }
        }
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) return

    val imageCapture = remember { ImageCapture.Builder().build() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var capturing by remember { mutableStateOf(false) }

    DisposableEffect(previewView, lifecycleOwner) {
        val view = previewView
        if (view == null) {
            onDispose {}
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    val cameraProvider = future.get()
                    provider = cameraProvider
                    val preview = Preview.Builder().build()
                    preview.surfaceProvider = view.surfaceProvider
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to bind camera", e)
                        onError()
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
            onDispose { provider?.unbindAll() }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> PreviewView(ctx).also { previewView = it } },
        )
        IconButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            onClick = {
                if (!capturing) {
                    capturing = true
                    capturePhoto(context, imageCapture, onPhotoTaken, onError)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Take photo", // TODO content description
                tint = Color.White, // TODO color
            )
        }
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (Path) -> Unit,
    onError: () -> Unit,
) {
    val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSuccess(Path(file.absolutePath))
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Failed to capture photo", exception)
                onError()
            }
        },
    )
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
