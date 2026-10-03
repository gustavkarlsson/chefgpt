package se.gustavkarlsson.chefgpt.agent.tools

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.agent.convertrecipeunits.ConvertRecipeUnitsAgent
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.recipes.FakeRecipeClient
import se.gustavkarlsson.chefgpt.recipes.InMemoryRecipePersistence
import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.Spoonacular
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveRecipeToolTest {
    private val userId = UserId.random()
    private val repository = RecipeRepository(InMemoryRecipePersistence())
    private val spoonacular = Spoonacular(FakeRecipeClient(), chefGptJson(strict = false))

    @Test
    fun `saves the looked-up recipe with converted units`() =
        runTest {
            val converted = listOf(ApiRecipeIngredient("spaghetti", "14.1", "oz"))
            val tool = SaveRecipeTool(repository, spoonacular, SaveConvertingAgent(converted), userId)

            val summary = tool.saveRecipe(716429)

            assertEquals(converted, repository.getRecipe(userId, summary.id)?.ingredients)
        }
}

private class SaveConvertingAgent(
    private val ingredients: List<ApiRecipeIngredient>,
) : ConvertRecipeUnitsAgent {
    override suspend fun convert(
        userId: UserId,
        recipe: NewRecipe,
    ): NewRecipe = recipe.copy(ingredients = ingredients)
}
