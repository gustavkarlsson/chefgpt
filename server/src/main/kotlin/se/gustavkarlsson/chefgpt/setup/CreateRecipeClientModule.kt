package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.recipes.FakeRecipeClient
import se.gustavkarlsson.chefgpt.recipes.RecipeClient
import se.gustavkarlsson.chefgpt.recipes.SpoonacularClient

fun createRecipeClientModule(config: ApplicationConfig) =
    module {
        single {
            when (val recipes = config.property("chefgpt.recipes").getString()) {
                "spoonacular" -> {
                    val spoonacularApiKey = config.property("chefgpt.spoonacularApiKey").getString()
                    SpoonacularClient(spoonacularApiKey)
                }

                "fake" -> {
                    FakeRecipeClient()
                }

                else -> {
                    error("Unknown recipes option: '$recipes'. Must be 'spoonacular' or 'fake'.")
                }
            }
        } bind RecipeClient::class
    }
