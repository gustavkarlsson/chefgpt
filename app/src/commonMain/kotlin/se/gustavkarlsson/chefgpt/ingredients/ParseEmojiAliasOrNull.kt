package se.gustavkarlsson.chefgpt.ingredients

private val parentheticalRegex = Regex("""\([^)]*\)""")
private val wordSeparatorRegex = Regex("""[\s\-]+""")
private val esSuffixes = listOf("ses", "xes", "zes", "ches", "shes", "oes")

private val diacriticFolds =
    buildMap {
        fun putFolded(
            plain: Char,
            accented: String,
        ) = accented.forEach { put(it, plain) }
        putFolded('a', "àáâãäåā")
        putFolded('c', "çćč")
        putFolded('e', "èéêëēėę")
        putFolded('i', "îïíīįì")
        putFolded('n', "ñń")
        putFolded('o', "ôöòóøōõ")
        putFolded('u', "ûüùúū")
        putFolded('y', "ÿý")
        putFolded('s', "śš")
        putFolded('z', "žźż")
        putFolded('g', "ğ")
        putFolded('l', "ł")
    }

/**
 * Attempts to parse an emoji alias (e.g. "banana") from the given [text] (e.g. "banana-split" or "bananas").
 * Valid emoji aliass are passed in [database].
 *
 * The [database] entries are always singular, contain only English-alphabet letters (no diacritics), and may be
 * hyphenated where the hyphen stands in for whitespace (e.g. "water-melon" matches the input "water melon").
 *
 * Breaks the input into words and tries to find the best match based on common English language rules.
 */
fun parseEmojiAliasOrNull(
    text: String,
    database: Set<String>,
): String? {
    val words =
        text
            .replace(parentheticalRegex, " ")
            .lowercase()
            .let(::foldDiacritics)
            .split(wordSeparatorRegex)
            .map(::normalizeWord)
            .filter(String::isNotBlank)

    return candidatePhrases(words).firstOrNull(database::contains)
}

private fun foldDiacritics(text: String): String =
    buildString {
        text.forEach { char ->
            when (char) {
                'æ' -> append("ae")
                'œ' -> append("oe")
                'ß' -> append("ss")
                else -> append(diacriticFolds[char] ?: char)
            }
        }
    }

private fun normalizeWord(word: String): String =
    word
        .removeSuffix("'s")
        .removeSuffix("’s")
        .trim('\'', '’', '.', ',')

/**
 * Yields candidate database keys for [words], most specific first: longer word spans before shorter ones, and
 * head-final spans (closer to the end) before earlier ones. Spans are joined with hyphens to match multi-word
 * database entries, and only the head (last) word is singularized, as English pluralizes the head noun.
 */
private fun candidatePhrases(words: List<String>): Sequence<String> =
    sequence {
        for (length in words.size downTo 1) {
            for (start in words.size - length downTo 0) {
                val span = words.subList(start, start + length)
                val prefix = span.dropLast(1)
                for (head in singularCandidates(span.last())) {
                    yield((prefix + head).joinToString("-"))
                }
            }
        }
    }

private fun singularCandidates(word: String): List<String> =
    buildList {
        add(word)
        when {
            word.endsWith("ies") && word.length > 3 -> {
                add(word.dropLast(3) + "y")
            }

            word.endsWith("ves") && word.length > 3 -> {
                add(word.dropLast(3) + "f")
                add(word.dropLast(3) + "fe")
            }

            esSuffixes.any(word::endsWith) -> {
                add(word.dropLast(2))
            }

            word.endsWith("s") && !word.endsWith("ss") -> {
                add(word.dropLast(1))
            }
        }
    }
