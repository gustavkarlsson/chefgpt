package se.gustavkarlsson.chefgpt

import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import java.nio.file.Paths

actual class File actual constructor(
    actual val path: String,
) {
    private val javaPath = Paths.get(path)

    actual fun readChannel(): ByteReadChannel = javaPath.toFile().readChannel()
}
