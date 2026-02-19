package se.gustavkarlsson.chefgpt

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.websocket.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.gustavkarlsson.chefgpt.api.MessageFromUser
import java.nio.file.Files
import kotlin.io.path.writeBytes

suspend fun WebSocketServerSession.receiveMessage(): MessageToAi {
    val messageFromUser = receiveDeserialized<MessageFromUser>()
    return convert(messageFromUser)
}

private suspend fun WebSocketServerSession.convert(content: MessageFromUser): MessageToAi {
    val image = content.image?.let { image ->
        val frame = incoming.receive()
        require(frame is Frame.Binary) {
            "Expected a binary frame, got ${frame.frameType}"
        }
        withContext(Dispatchers.IO) {
            val dir = Files.createTempDirectory("chefgpt-user-uploads")
            dir.resolve(image.fileName).also {
                it.writeBytes(frame.data)
            }
        }
    }
    return MessageToAi(content.text, image)
}
