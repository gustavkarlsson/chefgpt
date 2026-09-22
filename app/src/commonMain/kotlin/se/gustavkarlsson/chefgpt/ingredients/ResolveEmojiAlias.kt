package se.gustavkarlsson.chefgpt.ingredients

fun interface ResolveEmojiAlias {
    suspend operator fun invoke(emoji: String): String?
}

class RealResolveEmojiAlias(
    private val factory: IngredientEmojiResolver.Factory,
) : ResolveEmojiAlias {
    override suspend fun invoke(emoji: String): String? = factory.create().resolveAlias(emoji)
}
