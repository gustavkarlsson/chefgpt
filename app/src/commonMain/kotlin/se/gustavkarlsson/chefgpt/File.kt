package se.gustavkarlsson.chefgpt

expect class File(
    path: String,
) {
    val path: String
    val name: String

    suspend fun readBytes(): ByteArray
}
