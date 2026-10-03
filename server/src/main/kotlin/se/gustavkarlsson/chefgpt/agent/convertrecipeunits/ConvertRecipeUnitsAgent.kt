package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.recipes.NewRecipe

interface ConvertRecipeUnitsAgent {
    // Returns the recipe with its units converted to the user's preferences. Parts that cannot
    // be converted are kept with their original units.
    suspend fun convert(
        userId: UserId,
        recipe: NewRecipe,
    ): NewRecipe
}
