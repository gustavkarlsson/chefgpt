package se.gustavkarlsson.chefgpt

// No camera support for desktop for now
actual fun deviceSupportsCamera(): Boolean = false

actual fun deviceSupportsFilePicker(): Boolean = true

actual val devicePlatform: Platform = Platform.Desktop
