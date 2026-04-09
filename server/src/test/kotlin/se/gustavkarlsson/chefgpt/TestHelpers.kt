package se.gustavkarlsson.chefgpt

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import se.gustavkarlsson.chefgpt.api.ApiChat

const val VALID_USERNAME = "testuser"
const val VALID_PASSWORD = "Test123!"

suspend fun ApplicationTestBuilder.registerUser(
    username: String = VALID_USERNAME,
    password: String = VALID_PASSWORD,
): String {
    val client =
        createClient {
            expectSuccess = true
        }
    val response =
        client.post("/register") {
            basicAuth(username, password)
        }
    return checkNotNull(response.headers["Session-Id"]) {
        "Session-Id header missing from register response"
    }
}

suspend fun ApplicationTestBuilder.createChat(sessionId: String): ApiChat {
    val setupClient =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }
    val response =
        setupClient.post("/chats") {
            header("Session-Id", sessionId)
        }
    return response.body<ApiChat>()
}

suspend fun ApplicationTestBuilder.addIngredients(
    sessionId: String,
    vararg ingredients: String,
) {
    val client =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }
    for (ingredient in ingredients) {
        client.put("/ingredients/$ingredient") {
            header("Session-Id", sessionId)
        }
    }
}
