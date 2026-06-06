package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.kodein.emoji.Emoji
import org.kodein.emoji.list
import se.gustavkarlsson.chefgpt.IoOrDefault

/**
 * Resolves an [Emoji] for a free-text ingredient name by matching it against the short-code aliases of the full
 * Emoji.kt catalog.
 *
 * Building the catalog is expensive, so it is loaded once on a background dispatcher and cached.
 *
 * Emoji render with the platform's native font, which doesn't cover every emoji. [isSupported] decides whether an
 * emoji actually renders on the current platform; when `null`, every emoji is assumed supported.
 */
class IngredientEmojiResolver(
    scope: CoroutineScope = GlobalScope,
    dispatcher: CoroutineDispatcher = Dispatchers.IoOrDefault,
    private val isSupported: ((Emoji) -> Boolean)? = null,
) {
    private val byAlias: Deferred<Map<String, Emoji>> =
        scope.async(start = CoroutineStart.LAZY) {
            withContext(dispatcher) {
                val supported = Emoji.list().filter { isSupported?.invoke(it) ?: true }
                buildMap {
                    fun addIfAbsent(
                        key: String,
                        emoji: Emoji,
                    ) {
                        if (!containsKey(key)) put(key, emoji)
                    }
                    // Exact aliases take precedence.
                    supported.forEach { emoji ->
                        emoji.details.aliases.forEach { alias -> addIfAbsent(alias, emoji) }
                    }
                    // Many emoji are qualified (e.g. "red-apple"), so also index the bare head noun ("apple") to let
                    // single-word ingredient names resolve, without overriding an exact alias.
                    supported.forEach { emoji ->
                        emoji.details.aliases.forEach { alias ->
                            val head = alias.substringAfterLast('-')
                            if (head != alias) addIfAbsent(head, emoji)
                        }
                    }
                }
            }
        }

    suspend fun resolve(name: String): Emoji? {
        val map = byAlias.await()
        return parseEmojiAliasOrNull(name, map.keys)?.let(map::get)
    }
}
