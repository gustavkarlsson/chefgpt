package se.gustavkarlsson.chefgpt.agent.scaningredients

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportIngredientsToolTest {
    @Test
    fun `stores the reported ingredients`() =
        runTest {
            val tool = ReportIngredientsTool()

            tool.reportIngredients(listOf("tomatoes", "eggs"))

            assertEquals(listOf("tomatoes", "eggs"), tool.ingredients)
        }

    @Test
    fun `stores an empty report as no ingredients`() =
        runTest {
            val tool = ReportIngredientsTool()

            tool.reportIngredients(emptyList())

            assertEquals(emptyList(), tool.ingredients)
        }
}
