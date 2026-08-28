package se.gustavkarlsson.chefgpt

import kotlinx.io.files.Path
import java.awt.FileDialog
import java.awt.Frame

actual suspend fun pickFiles(multiple: Boolean): List<Path> {
    val fileDialog = FileDialog(null as Frame?, "Select Files", FileDialog.LOAD)
    fileDialog.isMultipleMode = multiple
    fileDialog.setFilenameFilter { _, name -> isPickable(name) }
    fileDialog.isVisible = true // Blocks
    return fileDialog.files.map { Path(it.absolutePath) }
}
