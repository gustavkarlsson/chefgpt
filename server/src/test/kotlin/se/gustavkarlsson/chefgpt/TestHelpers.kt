package se.gustavkarlsson.chefgpt

import io.ktor.client.request.basicAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import se.gustavkarlsson.chefgpt.api.ChatId

const val VALID_USERNAME = "testuser"
const val VALID_PASSWORD = "Test123!"

suspend fun ApplicationTestBuilder.registerUser(
    username: String = VALID_USERNAME,
    password: String = VALID_PASSWORD,
): String {
    val client = createClient {}
    val response =
        client.post("/register") {
            basicAuth(username, password)
        }
    return checkNotNull(response.headers["Session-Id"]) {
        "Session-Id header missing from register response"
    }
}

suspend fun ApplicationTestBuilder.createChat(sessionId: String): ChatId {
    val setupClient = createClient {}
    val response =
        setupClient.post("/chats") {
            header("Session-Id", sessionId)
        }
    return ChatId.parse(response.bodyAsText())
}
