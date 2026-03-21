package se.gustavkarlsson.chefgpt

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.websocket.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import java.nio.file.Files
import kotlin.io.path.writeBytes

suspend fun WebSocketServerSession.receiveMessage(): UserMessage? {
    val apiMessage = receiveDeserialized<ApiUserMessage>()
    return convert(apiMessage)
}

private suspend fun WebSocketServerSession.convert(apiMessage: ApiUserMessage): UserMessage? =
    when (apiMessage) {
        is ApiUserMessage.Regular -> {
            val image =
                apiMessage.imageFileId?.let { image ->
                    val frame = incoming.receive()
                    require(frame is Frame.Binary) {
                        "Expected a binary frame, got ${frame.frameType}"
                    }
                    withContext(Dispatchers.IO) {
                        // FIXME use image store
                        val dir = Files.createTempDirectory("chefgpt-user-uploads")
                        dir.resolve(image.value.toString()).also {
                            it.writeBytes(frame.data)
                        }
                    }
                }
            UserMessage(apiMessage.text, image)
        }

        ApiUserMessage.Waiting -> {
            null
        }
    }
