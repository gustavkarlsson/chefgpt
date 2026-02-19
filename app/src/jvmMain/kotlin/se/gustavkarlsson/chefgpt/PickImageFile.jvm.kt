package se.gustavkarlsson.chefgpt

import java.awt.FileDialog
import java.awt.Frame
import kotlin.io.path.Path
import kotlin.io.path.pathString

actual suspend fun pickImageFile(): File? {
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
        val path = Path(directory, file)
        File(path.pathString)
    } else {
        null
    }
}
