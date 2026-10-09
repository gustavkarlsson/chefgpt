package se.gustavkarlsson.chefgpt

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual val SERVER_BASE_URL: String = "http://localhost:8080"

actual val APP_STORAGE_DIR: String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: "."

actual val BASE_URL_HINT: String? = null

@OptIn(ExperimentalNativeApi::class)
actual val IS_DEBUG_BUILD: Boolean = Platform.isDebugBinary
