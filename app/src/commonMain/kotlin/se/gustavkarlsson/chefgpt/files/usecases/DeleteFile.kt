package se.gustavkarlsson.chefgpt.files.usecases

import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path

fun interface DeleteFile {
    operator fun invoke(path: Path)
}

class RealDeleteFile(
    private val fileSystem: FileSystem,
) : DeleteFile {
    override operator fun invoke(path: Path) {
        try {
            fileSystem.delete(path, mustExist = false)
        } catch (_: Exception) {
            // Best-effort cleanup; the cache dir is reclaimed by the OS anyway.
        }
    }
}
