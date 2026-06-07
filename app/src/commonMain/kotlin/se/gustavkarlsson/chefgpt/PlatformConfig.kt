package se.gustavkarlsson.chefgpt

// Base URL of the ChefGPT server. Differs per platform because, for example,
// the Android emulator reaches the host machine via 10.0.2.2 rather than localhost.
expect val SERVER_BASE_URL: String

// Writable directory where the app persists local state (last session, event history).
expect val APP_STORAGE_DIR: String
