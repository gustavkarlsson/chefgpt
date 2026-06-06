package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.kodein.emoji.Emoji
import org.kodein.emoji.list
import se.gustavkarlsson.chefgpt.IoOrDefault

/**
 * Resolves an [Emoji] for a free-text ingredient name by matching it against the short-code aliases of the full
 * Emoji.kt catalog.
 *
 * Building the catalog is expensive, so it is loaded once on a background dispatcher and cached.
 */
class IngredientEmojiResolver {
    private val byAlias: Deferred<Map<String, Emoji>> =
        CoroutineScope(Dispatchers.IoOrDefault).async(start = CoroutineStart.LAZY) {
            Emoji
                .list()
                .filter { it.details.hasNotoImage }
                .flatMap { emoji -> emoji.details.aliases.map { alias -> alias to emoji } }
                .toMap()
        }

    suspend fun resolve(name: String): Emoji? {
        val map = byAlias.await()
        return parseEmojiAliasOrNull(name, map.keys)?.let(map::get)
    }
}
