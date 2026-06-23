package se.gustavkarlsson.chefgpt

import android.os.Build

actual val SERVER_BASE_URL: String = guessDevServerBaseUrl()

actual val APP_STORAGE_DIR: String
    get() = ChefGptApplication.context.filesDir.absolutePath

actual val BASE_URL_HINT: String? = "Physical device? adb reverse tcp:8080 tcp:8080"

// Best guess for the dev server URL based on whether we run on an emulator.
// Emulators reach the host machine via the special loopback 10.0.2.2,
// while physical devices port-forwarded over adb reach it via localhost.
private fun guessDevServerBaseUrl(): String {
    val host = if (isProbablyEmulator()) "10.0.2.2" else "localhost"
    return "http://$host:8080"
}

private fun isProbablyEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
        Build.MODEL.contains("google_sdk", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
        Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
        Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
        Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
        Build.PRODUCT.contains("sdk", ignoreCase = true) ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
