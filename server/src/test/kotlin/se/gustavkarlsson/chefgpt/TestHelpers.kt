package se.gustavkarlsson.chefgpt

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.api.ApiIngredient
import se.gustavkarlsson.chefgpt.api.ApiIngredientUpdate
import se.gustavkarlsson.chefgpt.api.ApiNewIngredient
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.IngredientId
import se.gustavkarlsson.chefgpt.api.SpoonacularId

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

suspend fun ApplicationTestBuilder.createIngredients(
    sessionId: String,
    vararg ingredients: String,
): List<ApiIngredient> {
    val client =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }
    return ingredients.map { ingredient ->
        client
            .post("/ingredients") {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiNewIngredient(ingredient))
            }.body<ApiIngredient>()
    }
}

suspend fun ApplicationTestBuilder.saveRecipeSummary(
    sessionId: String,
    spoonacularId: SpoonacularId,
): ApiRecipeSummary {
    val client =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }
    return client
        .post("/recipe-summaries") {
            header("Session-Id", sessionId)
            contentType(ContentType.Application.Json)
            setBody(ApiSaveSpoonacularRecipe(spoonacularId))
        }.body<ApiRecipeSummary>()
}

suspend fun ApplicationTestBuilder.setIngredientInventory(
    sessionId: String,
    id: IngredientId,
    inInventory: Boolean,
): ApiIngredient {
    val client =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }
    return client
        .patch("/ingredients/$id") {
            header("Session-Id", sessionId)
            contentType(ContentType.Application.Json)
            setBody(ApiIngredientUpdate(inInventory))
        }.body<ApiIngredient>()
}
