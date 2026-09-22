package se.gustavkarlsson.chefgpt.ingredients.usecases

import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver

fun interface ResolveEmojiAlias {
    suspend operator fun invoke(emoji: String): String?
}

class RealResolveEmojiAlias(
    private val factory: IngredientEmojiResolver.Factory,
) : ResolveEmojiAlias {
    override suspend fun invoke(emoji: String): String? = factory.create().resolveAlias(emoji)
}
