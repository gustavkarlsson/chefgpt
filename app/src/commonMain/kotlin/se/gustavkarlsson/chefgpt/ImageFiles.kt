package se.gustavkarlsson.chefgpt

private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")

fun isImageFile(fileName: String): Boolean = fileName.extension() in imageExtensions

private fun String.extension(): String = substringAfterLast('.', "").lowercase()
