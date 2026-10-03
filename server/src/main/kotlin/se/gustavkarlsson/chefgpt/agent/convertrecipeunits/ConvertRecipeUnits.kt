package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.recipes.NewRecipe

fun interface ConvertRecipeUnits {
    suspend operator fun invoke(
        recipe: NewRecipe,
        facts: UserFacts,
    ): ConvertedMeasurements
}
