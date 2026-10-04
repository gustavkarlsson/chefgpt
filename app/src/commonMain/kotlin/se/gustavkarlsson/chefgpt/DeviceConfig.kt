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
    val supportsCamera: Boolean,
    val supportsFilePicker: Boolean,
) {
    // Whether at least one photo source (camera or file picker) is available.
    val supportsAnyPhotoSource: Boolean
        get() = supportsCamera || supportsFilePicker
}

fun readDeviceConfig(): DeviceConfig =
    DeviceConfig(
        platform = devicePlatform,
        supportsCamera = deviceSupportsCamera(),
        supportsFilePicker = deviceSupportsFilePicker(),
    )

expect fun deviceSupportsCamera(): Boolean

expect fun deviceSupportsFilePicker(): Boolean

expect val devicePlatform: Platform

enum class Platform {
    Android,
    Desktop,
    Ios,
    Web,
}

// Bumped manually with every client release; sent as the Client-Version header.
const val CLIENT_VERSION = "1.0"

// The value every request sends in the Client-Platform header.
val Platform.clientHeaderValue: String
    get() =
        when (this) {
            Platform.Android -> "android"
            Platform.Desktop -> "desktop"
            Platform.Ios -> "ios"
            Platform.Web -> "web"
        }
