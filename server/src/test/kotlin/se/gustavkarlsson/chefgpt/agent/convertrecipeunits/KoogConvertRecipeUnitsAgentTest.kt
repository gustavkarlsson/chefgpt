package se.gustavkarlsson.chefgpt.agent.convertrecipeunits

import kotlinx.coroutines.test.runTest
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.InMemoryFactRepository
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.VolumeUnits
import se.gustavkarlsson.chefgpt.facts.WeightUnits
import se.gustavkarlsson.chefgpt.recipes.NewRecipe
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val INGREDIENTS = listOf(ApiRecipeIngredient("chicken breast", "500", "g"))
private val STEPS = listOf("Bake at 350F for 20 minutes.")
private val CONVERTED_INGREDIENTS = listOf(ApiRecipeIngredient("chicken breast", "1.1", "lb"))
private val CONVERTED_STEPS = listOf("Bake at 175C for 20 minutes.")
private const val CONVERTED_DESCRIPTION = "Bake at 175C."

private fun recipe(description: String? = "A recipe.") =
    NewRecipe(
        title = "Baked chicken",
        steps = STEPS,
        imageUrl = null,
        description = description,
        preparationDuration = null,
        cookingDuration = null,
        duration = null,
        servings = null,
        ingredients = INGREDIENTS,
        nutrients = emptyList(),
        spoonacularId = null,
    )

private class FakeConvertRecipeUnits(
    private val result: ConvertedMeasurements,
) : ConvertRecipeUnits {
    var receivedRecipe: NewRecipe? = null
        private set
    var receivedFacts: UserFacts? = null
        private set

    override suspend fun invoke(
        recipe: NewRecipe,
        facts: UserFacts,
    ): ConvertedMeasurements {
        receivedRecipe = recipe
        receivedFacts = facts
        return result
    }
}

class KoogConvertRecipeUnitsAgentTest {
    private val userId = UserId.random()

    private val facts =
        UserFacts(
            preferredName = "Gustav",
            temperatureUnit = TemperatureUnit.Celsius,
            weightUnits = WeightUnits.UsImperial,
            volumeUnits = VolumeUnits.Metric,
            dietary = null,
        )

    @Test
    fun `converts ingredients, description and steps`() =
        runTest {
            val seam =
                FakeConvertRecipeUnits(
                    ConvertedMeasurements(
                        ingredients = CONVERTED_INGREDIENTS,
                        description = CONVERTED_DESCRIPTION,
                        steps = CONVERTED_STEPS,
                    ),
                )
            val agent = KoogConvertRecipeUnitsAgent(seam, factRepository(facts))

            val converted = agent.convert(userId, recipe())

            assertEquals(CONVERTED_INGREDIENTS, converted.ingredients)
            assertEquals(CONVERTED_DESCRIPTION, converted.description)
            assertEquals(CONVERTED_STEPS, converted.steps)
        }

    @Test
    fun `keeps the original description when its conversion fails`() =
        runTest {
            val seam =
                FakeConvertRecipeUnits(
                    ConvertedMeasurements(
                        ingredients = CONVERTED_INGREDIENTS,
                        description = null,
                        steps = CONVERTED_STEPS,
                    ),
                )
            val agent = KoogConvertRecipeUnitsAgent(seam, factRepository(facts))

            val converted = agent.convert(userId, recipe(description = "Original description."))

            assertEquals(CONVERTED_INGREDIENTS, converted.ingredients)
            assertEquals("Original description.", converted.description)
            assertEquals(CONVERTED_STEPS, converted.steps)
        }

    @Test
    fun `keeps the original ingredients when their conversion fails`() =
        runTest {
            val seam =
                FakeConvertRecipeUnits(
                    ConvertedMeasurements(
                        ingredients = null,
                        description = CONVERTED_DESCRIPTION,
                        steps = CONVERTED_STEPS,
                    ),
                )
            val agent = KoogConvertRecipeUnitsAgent(seam, factRepository(facts))

            val converted = agent.convert(userId, recipe())

            assertEquals(INGREDIENTS, converted.ingredients)
        }

    @Test
    fun `keeps a null description even when the converter returns one`() =
        runTest {
            val seam =
                FakeConvertRecipeUnits(
                    ConvertedMeasurements(
                        ingredients = CONVERTED_INGREDIENTS,
                        description = CONVERTED_DESCRIPTION,
                        steps = CONVERTED_STEPS,
                    ),
                )
            val agent = KoogConvertRecipeUnitsAgent(seam, factRepository(facts))

            val converted = agent.convert(userId, recipe(description = null))

            assertNull(converted.description)
        }

    @Test
    fun `passes the user's facts to the converter`() =
        runTest {
            val seam =
                FakeConvertRecipeUnits(
                    ConvertedMeasurements(
                        ingredients = CONVERTED_INGREDIENTS,
                        description = CONVERTED_DESCRIPTION,
                        steps = CONVERTED_STEPS,
                    ),
                )
            val agent = KoogConvertRecipeUnitsAgent(seam, factRepository(facts))

            agent.convert(userId, recipe())

            assertEquals(facts, seam.receivedFacts)
        }

    private fun factRepository(facts: UserFacts) =
        InMemoryFactRepository(ConcurrentHashMap(mutableMapOf(userId to facts)))
}
