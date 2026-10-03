package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.recipes.NewRecipe

class FakeConvertRecipeUnitsAgent : ConvertRecipeUnitsAgent {
    override suspend fun convert(
        userId: UserId,
        recipe: NewRecipe,
    ): NewRecipe = recipe
}
