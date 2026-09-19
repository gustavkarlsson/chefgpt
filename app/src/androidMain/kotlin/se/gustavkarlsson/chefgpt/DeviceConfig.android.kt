package se.gustavkarlsson.chefgpt

import android.content.pm.PackageManager

actual fun deviceSupportsCamera(): Boolean =
    ChefGptApplication.context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

actual val devicePlatform: Platform = Platform.Android
