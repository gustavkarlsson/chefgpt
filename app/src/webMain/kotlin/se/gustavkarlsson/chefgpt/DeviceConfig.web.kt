package se.gustavkarlsson.chefgpt

actual fun deviceSupportsCamera(): Boolean = false

actual fun deviceSupportsFilePicker(): Boolean = false

actual val devicePlatform: Platform = Platform.Web
