package se.gustavkarlsson.chefgpt.screens.camera

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.io.files.Path

// TODO Add result passing to Navigator instead of this mailbox.
class PhotoCaptureCoordinator {
    private val photosChannel = Channel<Path>(Channel.BUFFERED)

    val photos: Flow<Path> = photosChannel.receiveAsFlow()

    fun publish(path: Path) {
        photosChannel.trySend(path)
    }
}
