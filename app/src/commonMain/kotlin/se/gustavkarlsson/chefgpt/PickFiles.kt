package se.gustavkarlsson.chefgpt

import kotlinx.io.files.Path

/**
 * Lets the user pick files to attach. Only the types the agent can read are offered.
 */
expect suspend fun pickFiles(multiple: Boolean): List<Path>

private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")

private val documentExtensions = setOf("pdf", "txt", "csv")

fun isPickable(fileName: String): Boolean = fileName.extension() in imageExtensions + documentExtensions

fun isImageFile(fileName: String): Boolean = fileName.extension() in imageExtensions

private fun String.extension(): String = substringAfterLast('.', "").lowercase()
