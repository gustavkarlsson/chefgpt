package se.gustavkarlsson.chefgpt.setup

import io.ktor.server.application.Application
import org.koin.dsl.bind
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.agent.scraperecipe.ScrapeRecipeAgent
import se.gustavkarlsson.chefgpt.recipes.JsonLdScrapeRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeImageRehoster
import se.gustavkarlsson.chefgpt.recipes.RecipeJsonLdParser
import se.gustavkarlsson.chefgpt.recipes.RecipeScraper
import se.gustavkarlsson.chefgpt.recipes.SaveRecipeFromUrl
import se.gustavkarlsson.chefgpt.recipes.Spoonacular
import se.gustavkarlsson.chefgpt.recipes.SpoonacularScrapeRecipe
import se.gustavkarlsson.chefgpt.recipes.TieredScrapeRecipe

fun Application.createRecipesModule() =
    module {
        single { Spoonacular(get(), get()) }
        single { RecipeJsonLdParser(get()) }
        single { RecipeImageRehoster(get()) }
        single { SpoonacularScrapeRecipe(get()) }
        single { JsonLdScrapeRecipe(get(), get()) }
        single {
            TieredScrapeRecipe(
                listOf(
                    // get<SpoonacularScrapeRecipe>(),
                    get<JsonLdScrapeRecipe>(),
                    get<ScrapeRecipeAgent>(),
                ),
            )
        } bind RecipeScraper::class
        single { SaveRecipeFromUrl(get(), get(), get(), get()) }
    }
