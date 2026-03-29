package se.gustavkarlsson.chefgpt

import io.ktor.client.request.basicAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder

const val VALID_USERNAME = "testuser"
const val VALID_PASSWORD = "Test123!"

suspend fun ApplicationTestBuilder.registerUser(
    username: String = VALID_USERNAME,
    password: String = VALID_PASSWORD,
): String {
    val setupClient = createClient {}
    val response =
        setupClient.post("/register") {
            basicAuth(username, password)
        }
    return response.headers["Session-Id"]!!
}

suspend fun ApplicationTestBuilder.createChat(sessionId: String): String {
    val setupClient = createClient {}
    val response =
        setupClient.post("/chats") {
            header("Session-Id", sessionId)
        }
    return response.bodyAsText()
}
