package se.gustavkarlsson.chefgpt

import io.ktor.utils.io.ByteReadChannel

expect class File(
    path: String,
) {
    val path: String

    fun readChannel(): ByteReadChannel
}
