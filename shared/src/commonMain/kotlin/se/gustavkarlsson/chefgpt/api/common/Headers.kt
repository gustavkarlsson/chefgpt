package se.gustavkarlsson.chefgpt.api.common

// Carries the original file name on upload, which multipart would otherwise be needed for.
const val FILE_NAME_HEADER = "File-Name"

// Sent by every client on every request, so the server can apply client-specific workarounds.
const val CLIENT_PLATFORM_HEADER = "Client-Platform"
const val CLIENT_VERSION_HEADER = "Client-Version"
