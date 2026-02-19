package se.gustavkarlsson.chefgpt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import kotlin.io.path.readBytes

actual class File actual constructor(
    actual val path: String,
) {
    private val javaPath = Paths.get(path)

    actual val name: String = javaPath.fileName.toString()

    actual suspend fun readBytes(): ByteArray =
        withContext(Dispatchers.IO) {
            javaPath.readBytes()
        }
}
