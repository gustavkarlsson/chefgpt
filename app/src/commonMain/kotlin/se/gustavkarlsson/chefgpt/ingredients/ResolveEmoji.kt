package se.gustavkarlsson.chefgpt.ingredients

import org.kodein.emoji.Emoji

fun interface ResolveEmoji {
    suspend operator fun invoke(name: String): Emoji?
}

class RealResolveEmoji(
    private val factory: IngredientEmojiResolver.Factory,
) : ResolveEmoji {
    override suspend fun invoke(name: String): Emoji? = factory.create().resolve(name)
}
