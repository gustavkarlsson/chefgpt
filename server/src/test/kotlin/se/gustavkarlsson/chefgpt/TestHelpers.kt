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
import se.gustavkarlsson.chefgpt.api.auth.v1.AUTH_V1_PATH
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiChat
import se.gustavkarlsson.chefgpt.api.chats.v1.CHATS_V1_PATH
import se.gustavkarlsson.chefgpt.api.common.IngredientId
import se.gustavkarlsson.chefgpt.api.common.RecipeId
import se.gustavkarlsson.chefgpt.api.common.SpoonacularId
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiIngredient
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiIngredientUpdate
import se.gustavkarlsson.chefgpt.api.ingredients.v1.ApiNewIngredient
import se.gustavkarlsson.chefgpt.api.ingredients.v1.INGREDIENTS_V1_PATH
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiRecipeUpdate
import se.gustavkarlsson.chefgpt.api.recipes.v1.ApiSaveSpoonacularRecipe
import se.gustavkarlsson.chefgpt.api.recipes.v1.RECIPES_V1_PATH

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
        client.post("$AUTH_V1_PATH/register") {
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
            install(ContentNegotiation) { json(chefGptJson(strict = true)) }
        }
    val response =
        setupClient.post(CHATS_V1_PATH) {
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
            install(ContentNegotiation) { json(chefGptJson(strict = true)) }
        }
    return ingredients.map { ingredient ->
        client
            .post(INGREDIENTS_V1_PATH) {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(ApiNewIngredient(ingredient))
            }.body<ApiIngredient>()
    }
}

suspend fun ApplicationTestBuilder.saveRecipe(
    sessionId: String,
    spoonacularId: SpoonacularId,
): ApiRecipe {
    val client =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json(chefGptJson(strict = true)) }
        }
    return client
        .post(RECIPES_V1_PATH) {
            header("Session-Id", sessionId)
            contentType(ContentType.Application.Json)
            setBody(ApiSaveSpoonacularRecipe(spoonacularId))
        }.body<ApiRecipe>()
}

suspend fun ApplicationTestBuilder.setRecipeFavorite(
    sessionId: String,
    id: RecipeId,
    favorite: Boolean,
): ApiRecipe {
    val client =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json(chefGptJson(strict = true)) }
        }
    return client
        .patch("$RECIPES_V1_PATH/$id") {
            header("Session-Id", sessionId)
            contentType(ContentType.Application.Json)
            setBody(ApiRecipeUpdate(favorite))
        }.body<ApiRecipe>()
}

suspend fun ApplicationTestBuilder.setIngredientInventory(
    sessionId: String,
    id: IngredientId,
    inInventory: Boolean,
): ApiIngredient {
    val client =
        createClient {
            expectSuccess = true
            install(ContentNegotiation) { json(chefGptJson(strict = true)) }
        }
    return client
        .patch("$INGREDIENTS_V1_PATH/$id") {
            header("Session-Id", sessionId)
            contentType(ContentType.Application.Json)
            setBody(ApiIngredientUpdate(inInventory))
        }.body<ApiIngredient>()
}
