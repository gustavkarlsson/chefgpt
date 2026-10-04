package se.gustavkarlsson.chefgpt.recipes

import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresRecipePersistence(
    private val db: DatabaseAccess,
) : RecipePersistence {
    override suspend fun get(
        userId: UserId,
        id: RecipeId,
    ): Recipe? =
        db.use {
            selectRecipe(userId.value.toJavaUuid(), id.value.toJavaUuid())
        }

    override suspend fun insert(
        userId: UserId,
        recipe: NewRecipe,
        favorite: Boolean,
        modifiedFrom: RecipeId?,
    ): Recipe {
        val userUuid = userId.value.toJavaUuid()
        return db.use {
            transactionWithResult {
                val id = insertRecipe(userUuid, recipe, favorite, modifiedFrom)
                checkNotNull(selectRecipe(userUuid, id)) { "Saved recipe $id disappeared" }
            }
        }
    }

    override suspend fun replace(
        userId: UserId,
        recipe: Recipe,
    ): Recipe? {
        val userUuid = userId.value.toJavaUuid()
        val id = recipe.id.value.toJavaUuid()
        return db.use {
            transactionWithResult {
                val recipeId =
                    recipeQueries
                        .updateAllByUserIdAndId(
                            title = recipe.title,
                            imageUrl = recipe.imageUrl?.value,
                            description = recipe.description,
                            preparationMinutes = recipe.preparationDuration?.toMinutes(),
                            cookingMinutes = recipe.cookingDuration?.toMinutes(),
                            totalMinutes = recipe.duration?.toMinutes(),
                            minServings = recipe.servings?.first,
                            maxServings = recipe.servings?.last,
                            favorite = recipe.favorite,
                            modifiedFrom = recipe.modifiedFrom?.value?.toJavaUuid(),
                            userId = userUuid,
                            id = id,
                        ).executeAsOneOrNull()
                        ?: return@transactionWithResult null
                recipeQueries.deleteStepsByRecipeId(recipeId)
                insertSteps(recipeId, recipe.steps)
                recipeQueries.deleteIngredientsByRecipeId(recipeId)
                insertIngredients(recipeId, recipe.ingredients)
                recipeQueries.deleteNutrientsByRecipeId(recipeId)
                insertNutrients(recipeId, recipe.nutrients)
                selectRecipe(userUuid, recipeId)
            }
        }
    }

    override suspend fun delete(
        userId: UserId,
        id: RecipeId,
    ): Boolean =
        db.use {
            recipeQueries
                .deleteByUserIdAndId(userId.value.toJavaUuid(), id.value.toJavaUuid())
                .executeAsList()
                .isNotEmpty()
        }

    override suspend fun listSummaries(userId: UserId): List<RecipeSummary> =
        db.use {
            recipeQueries
                .selectSummariesByUserId(userId.value.toJavaUuid())
                .executeAsList()
                .map {
                    RecipeSummary(
                        id = RecipeId(it.id.toKotlinUuid()),
                        title = it.title,
                        spoonacularId = it.spoonacular_id?.let(::SpoonacularId),
                        imageUrl = it.image_url?.let(::ImageUrl),
                        favorite = it.favorite,
                        modifiedFrom = it.modified_from?.let { id -> RecipeId(id.toKotlinUuid()) },
                    )
                }
        }
}

private fun ChefGptDatabase.selectRecipe(
    userId: UUID,
    id: UUID,
): Recipe? {
    val recipe = recipeQueries.selectByUserIdAndId(userId, id).executeAsOneOrNull() ?: return null
    return Recipe(
        id = RecipeId(recipe.id.toKotlinUuid()),
        spoonacularId = recipe.spoonacular_id?.let(::SpoonacularId),
        title = recipe.title,
        imageUrl = recipe.image_url?.let(::ImageUrl),
        steps = recipeQueries.selectStepsByRecipeId(id).executeAsList(),
        favorite = recipe.favorite,
        modifiedFrom = recipe.modified_from?.let { RecipeId(it.toKotlinUuid()) },
        description = recipe.description,
        preparationDuration = recipe.preparation_minutes?.minutes,
        cookingDuration = recipe.cooking_minutes?.minutes,
        duration = recipe.total_minutes?.minutes,
        servings = toIntRangeOrNull(recipe.min_servings, recipe.max_servings),
        ingredients =
            recipeQueries
                .selectIngredientsByRecipeId(id)
                .executeAsList()
                .map { RecipeIngredient(it.name, it.amount, it.unit) },
        nutrients =
            recipeQueries
                .selectNutrientsByRecipeId(id)
                .executeAsList()
                .map { Nutrient(it.name, it.amount, it.unit) },
    )
}

private fun ChefGptDatabase.insertRecipe(
    userId: UUID,
    recipe: NewRecipe,
    favorite: Boolean,
    modifiedFrom: RecipeId?,
): UUID {
    val id =
        recipeQueries
            .insert(
                userId,
                recipe.spoonacularId?.value,
                recipe.title,
                recipe.imageUrl?.value,
                recipe.description,
                recipe.preparationDuration?.toMinutes(),
                recipe.cookingDuration?.toMinutes(),
                recipe.duration?.toMinutes(),
                recipe.servings?.first,
                recipe.servings?.last,
                favorite,
                modifiedFrom?.value?.toJavaUuid(),
            ).executeAsOne()
    insertSteps(id, recipe.steps)
    insertIngredients(id, recipe.ingredients)
    insertNutrients(id, recipe.nutrients)
    return id
}

private fun ChefGptDatabase.insertSteps(
    recipeId: UUID,
    steps: List<String>,
) = steps.forEachIndexed { index, step -> recipeQueries.insertStep(recipeId, index, step) }

private fun ChefGptDatabase.insertIngredients(
    recipeId: UUID,
    ingredients: List<RecipeIngredient>,
) = ingredients.forEachIndexed { index, ingredient ->
    recipeQueries.insertIngredient(recipeId, index, ingredient.name, ingredient.value, ingredient.unit)
}

private fun ChefGptDatabase.insertNutrients(
    recipeId: UUID,
    nutrients: List<Nutrient>,
) = nutrients.forEachIndexed { index, nutrient ->
    recipeQueries.insertNutrient(recipeId, index, nutrient.name, nutrient.value, nutrient.unit)
}

private fun Duration.toMinutes(): Int = inWholeMinutes.toInt()

// The two columns are written together, so only a complete pair makes a range.
private fun toIntRangeOrNull(
    min: Int?,
    max: Int?,
): IntRange? = if (min != null && max != null) min..max else null
