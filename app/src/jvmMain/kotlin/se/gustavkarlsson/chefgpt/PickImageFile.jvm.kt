package se.gustavkarlsson.chefgpt

import kotlinx.io.files.Path
import java.awt.FileDialog
import java.awt.Frame

actual suspend fun pickImageFile(): Path? {
    val fileDialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD)
    fileDialog.setFilenameFilter { _, name ->
        name.lowercase().endsWith(".png") ||
            name.lowercase().endsWith(".jpg") ||
            name.lowercase().endsWith(".jpeg") ||
            name.lowercase().endsWith(".gif") ||
            name.lowercase().endsWith(".bmp")
    }
    fileDialog.isVisible = true // Blocks
    val directory = fileDialog.directory
    val file = fileDialog.file
    return if (directory != null && file != null) {
        Path(directory, file)
    } else {
        null
    }
}
