package se.gustavkarlsson.chefgpt

// The emulator reaches the host machine running the dev server via 10.0.2.2.
actual val SERVER_BASE_URL: String = "http://10.0.2.2:8080"

actual val APP_STORAGE_DIR: String
    get() = ChefGptApplication.context.filesDir.absolutePath
