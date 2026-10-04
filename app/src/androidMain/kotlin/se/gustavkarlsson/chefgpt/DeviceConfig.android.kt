package se.gustavkarlsson.chefgpt

import android.content.pm.PackageManager

actual fun deviceSupportsCamera(): Boolean =
    ChefGptApplication.context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

// The system photo picker is available on every supported Android version.
actual fun deviceSupportsFilePicker(): Boolean = true

actual val devicePlatform: Platform = Platform.Android
