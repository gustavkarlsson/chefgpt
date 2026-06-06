package se.gustavkarlsson.chefgpt.ingredients

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kodein.emoji.Emoji
import org.kodein.emoji.list
import se.gustavkarlsson.chefgpt.IoOrDefault

/**
 * Resolves an [Emoji] for a free-text ingredient name by matching it against the short-code aliases of the
 * Emoji.kt catalog.
 *
 * Instances are created by [Factory], which loads and processes the (expensive) catalog up front so that
 * [resolve] never suspends.
 */
class IngredientEmojiResolver internal constructor(
    private val byAlias: Map<String, Emoji>,
    private val aliasByEmoji: Map<String, String>,
) {
    private val cache = mutableMapOf<String, Emoji?>()

    fun resolve(name: String): Emoji? =
        if (cache.containsKey(name)) {
            cache[name]
        } else {
            parseEmojiAliasOrNull(name, byAlias.keys)?.let(byAlias::get).also { cache[name] = it }
        }

    /**
     * Returns the short-code alias for a UTF-8 emoji glyph (e.g. "🍌" -> "banana"), or `null` if [emoji] is not a
     * known emoji. Independent of platform support, so a glyph still maps even when it doesn't render natively.
     */
    fun resolveAlias(emoji: String): String? = aliasByEmoji[emoji]

    /**
     * Loads and processes the Emoji.kt catalog to build [IngredientEmojiResolver] instances.
     *
     * The catalog is loaded once and the resulting resolver is memoized, so repeated [create] calls share a single
     * instance (and therefore a single cache).
     *
     * Emoji render with the platform's native font, which doesn't cover every emoji. [isSupported] decides whether
     * an emoji actually renders on the current platform; when `null`, every emoji is assumed supported.
     */
    class Factory(
        private val dispatcher: CoroutineDispatcher = Dispatchers.IoOrDefault,
        private val isSupported: ((Emoji) -> Boolean)? = null,
    ) {
        private val mutex = Mutex()
        private var instance: IngredientEmojiResolver? = null

        suspend fun create(): IngredientEmojiResolver =
            instance ?: mutex.withLock {
                instance ?: build().also { instance = it }
            }

        private suspend fun build(): IngredientEmojiResolver =
            withContext(dispatcher) {
                val all = Emoji.list()
                val supported = all.filter { isSupported?.invoke(it) ?: true }
                val byAlias =
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
                        // Many emoji are qualified (e.g. "red-apple"), so also index the bare head noun ("apple")
                        // to let single-word ingredient names resolve, without overriding an exact alias.
                        supported.forEach { emoji ->
                            emoji.details.aliases.forEach { alias ->
                                val head = alias.substringAfterLast('-')
                                if (head != alias) addIfAbsent(head, emoji)
                            }
                        }
                    }
                // Reverse lookup spans the full catalog (not just [supported]) so a glyph maps to its alias
                // regardless of whether the platform can render it.
                val aliasByEmoji =
                    buildMap {
                        all.forEach { emoji ->
                            val alias = emoji.details.aliases.firstOrNull() ?: return@forEach
                            if (!containsKey(emoji.details.string)) put(emoji.details.string, alias)
                        }
                    }
                IngredientEmojiResolver(byAlias, aliasByEmoji)
            }
    }
}
