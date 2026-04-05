package se.gustavkarlsson.chefgpt

import kotlinx.io.files.Path

expect suspend fun pickImageFile(): Path?
