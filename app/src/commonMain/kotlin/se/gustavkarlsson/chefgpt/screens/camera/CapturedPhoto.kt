package se.gustavkarlsson.chefgpt.screens.camera

import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.StringValueSerializer
import kotlin.jvm.JvmInline

/**
 * A photo captured by [CameraScreen], identified by the file it was written to.
 *
 * [Path] carries no serializer of its own, so it is wrapped here to serve as a
 * screen result type.
 */
@Serializable(with = CapturedPhotoSerializer::class)
@JvmInline
value class CapturedPhoto(
    val path: Path,
)

object CapturedPhotoSerializer : StringValueSerializer<CapturedPhoto>(
    "captured-photo",
    wrap = { CapturedPhoto(Path(it)) },
    unwrap = { it.path.toString() },
)
