package se.gustavkarlsson.chefgpt.recipes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import se.gustavkarlsson.chefgpt.api.ApiNutrient
import se.gustavkarlsson.chefgpt.api.ApiRecipe
import se.gustavkarlsson.chefgpt.api.ApiRecipeIngredient
import se.gustavkarlsson.chefgpt.api.ApiRecipeSummary
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.api.SpoonacularId
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.ChefGptDatabase
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import se.gustavkarlsson.chefgpt.util.RepoSyncer
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class PostgresRecipeStore(
    private val db: DatabaseAccess,
) : RecipeStore {
    private val syncer = RepoSyncer<UserId>()

    override suspend fun getRecipe(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? =
        db.use {
            transactionWithResult {
                selectRecipe(userId.value.toJavaUuid(), id.value.toJavaUuid())
            }
        }

    override suspend fun getRecipeSummaries(userId: UserId): List<ApiRecipeSummary> =
        db.use {
            recipeQueries
                .selectSummariesByUserId(userId.value.toJavaUuid())
                .executeAsList()
                .map {
                    ApiRecipeSummary(
                        id = RecipeId(it.id.toKotlinUuid()),
                        title = it.title,
                        spoonacularId = it.spoonacular_id?.let(::SpoonacularId),
                        imageUrl = it.image_url?.let(::ImageUrl),
                        favorite = it.favorite,
                        modifiedFrom = it.modified_from?.let { id -> RecipeId(id.toKotlinUuid()) },
                    )
                }
        }

    override fun streamRecipeSummaries(userId: UserId): Flow<List<ApiRecipeSummary>> =
        syncer
            .notifications(userId)
            .map { getRecipeSummaries(userId) }
            .distinctUntilChanged()

    override suspend fun saveRecipe(
        userId: UserId,
        recipe: NewRecipe,
    ): ApiRecipe {
        val userUuid = userId.value.toJavaUuid()
        val saved =
            db.use {
                transactionWithResult {
                    val id = insertRecipe(userUuid, recipe)
                    checkNotNull(selectRecipe(userUuid, id)) { "Saved recipe $id disappeared" }
                }
            }
        syncer.notifyChange(userId)
        return saved
    }

    override suspend fun modifyRecipe(
        userId: UserId,
        id: RecipeId,
        update: RecipeUpdate,
    ): ApiRecipe? {
        val userUuid = userId.value.toJavaUuid()
        val modified =
            db.use {
                transactionWithResult {
                    val base = selectRecipe(userUuid, id.value.toJavaUuid()) ?: return@transactionWithResult null
                    val modificationId =
                        if (base.modifiedFrom != null) {
                            base.id.value.toJavaUuid()
                        } else {
                            recipeQueries
                                .selectIdByUserIdAndModifiedFrom(userUuid, base.id.value.toJavaUuid())
                                .executeAsOneOrNull()
                        }
                    val modifiedId =
                        if (modificationId != null) {
                            updateRecipe(userUuid, modificationId, update)
                        } else {
                            insertRecipe(userUuid, base.applyUpdate(update).toNewRecipe(), base.favorite, base.id)
                        } ?: return@transactionWithResult null
                    selectRecipe(userUuid, modifiedId)
                }
            }
        if (modified != null) {
            syncer.notifyChange(userId)
        }
        return modified
    }

    override suspend fun overwriteOriginal(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? {
        val userUuid = userId.value.toJavaUuid()
        val overwritten =
            db.use {
                transactionWithResult {
                    val recipeUuid = id.value.toJavaUuid()
                    val originalId =
                        selectRecipe(userUuid, recipeUuid)?.modifiedFrom
                            ?: return@transactionWithResult null
                    // The self-referencing foreign key clears modified_from when the original goes.
                    recipeQueries.deleteByUserIdAndId(userUuid, originalId.value.toJavaUuid()).executeAsList()
                    selectRecipe(userUuid, recipeUuid)
                }
            }
        if (overwritten != null) {
            syncer.notifyChange(userId)
        }
        return overwritten
    }

    override suspend fun saveAsCopy(
        userId: UserId,
        id: RecipeId,
    ): ApiRecipe? {
        val userUuid = userId.value.toJavaUuid()
        val copy =
            db.use {
                transactionWithResult {
                    val recipeUuid = id.value.toJavaUuid()
                    if (selectRecipe(userUuid, recipeUuid)?.modifiedFrom == null) {
                        return@transactionWithResult null
                    }
                    recipeQueries.clearModifiedFromByUserIdAndId(userUuid, recipeUuid).executeAsOne()
                    selectRecipe(userUuid, recipeUuid)
                }
            }
        if (copy != null) {
            syncer.notifyChange(userId)
        }
        return copy
    }

    override suspend fun setFavorite(
        userId: UserId,
        id: RecipeId,
        favorite: Boolean,
    ): ApiRecipe? {
        val userUuid = userId.value.toJavaUuid()
        val updated =
            db.use {
                transactionWithResult {
                    val recipeId =
                        recipeQueries
                            .setFavoriteByUserIdAndId(favorite, userUuid, id.value.toJavaUuid())
                            .executeAsOneOrNull()
                            ?: return@transactionWithResult null
                    selectRecipe(userUuid, recipeId)
                }
            }
        if (updated != null) {
            syncer.notifyChange(userId)
        }
        return updated
    }

    override suspend fun deleteRecipe(
        userId: UserId,
        id: RecipeId,
    ): Boolean {
        val deleted =
            db.use {
                recipeQueries
                    .deleteByUserIdAndId(userId.value.toJavaUuid(), id.value.toJavaUuid())
                    .executeAsList()
                    .isNotEmpty()
            }
        if (deleted) {
            syncer.notifyChange(userId)
        }
        return deleted
    }
}

private fun ChefGptDatabase.selectRecipe(
    userId: UUID,
    id: UUID,
): ApiRecipe? {
    val recipe = recipeQueries.selectByUserIdAndId(userId, id).executeAsOneOrNull() ?: return null
    return ApiRecipe(
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
        ingredients =
            recipeQueries
                .selectIngredientsByRecipeId(id)
                .executeAsList()
                .map { ApiRecipeIngredient(it.name, it.amount, it.unit) },
        nutrients =
            recipeQueries
                .selectNutrientsByRecipeId(id)
                .executeAsList()
                .map { ApiNutrient(it.name, it.amount, it.unit) },
    )
}

private fun ChefGptDatabase.insertRecipe(
    userId: UUID,
    recipe: NewRecipe,
    favorite: Boolean = false,
    modifiedFrom: RecipeId? = null,
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
                favorite,
                modifiedFrom?.value?.toJavaUuid(),
            ).executeAsOne()
    insertSteps(id, recipe.steps)
    insertIngredients(id, recipe.ingredients)
    insertNutrients(id, recipe.nutrients)
    return id
}

// Applies the non-null parts of the update to the recipe in place, returning its id,
// or null if no recipe matched.
private fun ChefGptDatabase.updateRecipe(
    userId: UUID,
    id: UUID,
    update: RecipeUpdate,
): UUID? {
    val recipeId =
        recipeQueries
            .updateByUserIdAndId(
                update.title,
                update.imageUrl?.value,
                update.description,
                update.preparationDuration?.toMinutes(),
                update.cookingDuration?.toMinutes(),
                update.duration?.toMinutes(),
                userId,
                id,
            ).executeAsOneOrNull()
            ?: return null
    update.steps?.let {
        recipeQueries.deleteStepsByRecipeId(recipeId)
        insertSteps(recipeId, it)
    }
    update.ingredients?.let {
        recipeQueries.deleteIngredientsByRecipeId(recipeId)
        insertIngredients(recipeId, it)
    }
    update.nutrients?.let {
        recipeQueries.deleteNutrientsByRecipeId(recipeId)
        insertNutrients(recipeId, it)
    }
    return recipeId
}

private fun ChefGptDatabase.insertSteps(
    recipeId: UUID,
    steps: List<String>,
) = steps.forEachIndexed { index, step -> recipeQueries.insertStep(recipeId, index, step) }

private fun ChefGptDatabase.insertIngredients(
    recipeId: UUID,
    ingredients: List<ApiRecipeIngredient>,
) = ingredients.forEachIndexed { index, ingredient ->
    recipeQueries.insertIngredient(recipeId, index, ingredient.name, ingredient.value, ingredient.unit)
}

private fun ChefGptDatabase.insertNutrients(
    recipeId: UUID,
    nutrients: List<ApiNutrient>,
) = nutrients.forEachIndexed { index, nutrient ->
    recipeQueries.insertNutrient(recipeId, index, nutrient.name, nutrient.value, nutrient.unit)
}

private fun Duration.toMinutes(): Int = inWholeMinutes.toInt()
