package se.gustavkarlsson.chefgpt

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual val SERVER_BASE_URL: String = "http://localhost:8080"

actual val APP_STORAGE_DIR: String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: "."
