package se.gustavkarlsson.chefgpt

actual val SERVER_BASE_URL: String = "http://localhost:8080"

actual val APP_STORAGE_DIR: String = "."

actual val BASE_URL_HINT: String? = null

// TODO: Derive from the build type instead of hardcoding once desktop/web get release builds.
actual val IS_DEBUG_BUILD: Boolean = true
