package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.tools.Tool
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

interface IngredientStore {
    fun getIngredients(): List<String>

    fun addIngredients(ingredients: List<String>)

    fun removeIngredients(ingredients: List<String>)

    fun clearIngredients()
}

fun IngredientStore.asTools(): List<Tool<*, *>> =
    listOf(
        object : Tool<Unit, List<String>>(
            argsSerializer = Unit.serializer(),
            resultSerializer = ListSerializer(String.serializer()),
            name = "getIngredients",
            description = "Get all user's stored ingredients",
        ) {
            override suspend fun execute(args: Unit): List<String> = getIngredients()
        },
        object : Tool<List<String>, Unit>(
            argsSerializer = ListSerializer(String.serializer()),
            resultSerializer = Unit.serializer(),
            name = "addIngredients",
            description = "Add the given ingredients to the user's store",
        ) {
            override suspend fun execute(args: List<String>) = addIngredients(args)
        },
        object : Tool<List<String>, Unit>(
            argsSerializer = ListSerializer(String.serializer()),
            resultSerializer = Unit.serializer(),
            name = "removeIngredients",
            description = "Remove the given ingredients from the user's store",
        ) {
            override suspend fun execute(args: List<String>) = removeIngredients(args)
        },
        object : Tool<Unit, Unit>(
            argsSerializer = Unit.serializer(),
            resultSerializer = Unit.serializer(),
            name = "clearIngredients",
            description = "Clear all ingredients from the user's store",
        ) {
            override suspend fun execute(args: Unit) = clearIngredients()
        },
    )

expect fun createIngredientStore(): IngredientStore
