package se.gustavkarlsson.chefgpt

// Base URL of the ChefGPT server. Differs per platform because, for example,
// the Android emulator reaches the host machine via 10.0.2.2 rather than localhost.
expect val SERVER_BASE_URL: String

// Writable directory where the app persists local state (last session, event history).
expect val APP_STORAGE_DIR: String

// Optional hint shown under the base URL field. Null on platforms where it doesn't apply.
expect val BASE_URL_HINT: String?

data class DeviceConfig(
    val platform: Platform,
    // The device has a camera, so screens may offer to take a photo.
    val supportsCamera: Boolean,
    // CapturePhoto can produce photos at all. Implies [supportsCamera] on mobile, but
    // desktop has no camera and picks image files instead.
    val supportsPhotoCapture: Boolean,
)

fun readDeviceConfig(): DeviceConfig {
    val supportsCamera = deviceSupportsCamera()
    return DeviceConfig(
        platform = devicePlatform,
        supportsCamera = supportsCamera,
        supportsPhotoCapture =
            when (devicePlatform) {
                Platform.Android, Platform.Ios -> supportsCamera
                Platform.Desktop -> true
                Platform.Web -> false
            },
    )
}

expect fun deviceSupportsCamera(): Boolean

expect val devicePlatform: Platform

enum class Platform {
    Android,
    Desktop,
    Ios,
    Web,
}
