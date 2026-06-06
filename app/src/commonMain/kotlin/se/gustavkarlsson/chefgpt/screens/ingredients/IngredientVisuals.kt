package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

// Best-effort emoji for common ingredients. Anything not listed falls back to a
// colored circle with the ingredient's first letter.
// FIXME replace with more robust solution
private val ingredientEmojis: Map<String, String> =
    mapOf(
        "egg" to "🥚",
        "eggs" to "🥚",
        "milk" to "🥛",
        "butter" to "🧈",
        "cheese" to "🧀",
        "bread" to "🍞",
        "rice" to "🍚",
        "pasta" to "🍝",
        "chicken" to "🍗",
        "beef" to "🥩",
        "steak" to "🥩",
        "bacon" to "🥓",
        "fish" to "🐟",
        "shrimp" to "🦐",
        "tomato" to "🍅",
        "tomatoes" to "🍅",
        "onion" to "🧅",
        "garlic" to "🧄",
        "carrot" to "🥕",
        "carrots" to "🥕",
        "potato" to "🥔",
        "potatoes" to "🥔",
        "mushroom" to "🍄",
        "mushrooms" to "🍄",
        "broccoli" to "🥦",
        "corn" to "🌽",
        "pepper" to "🌶️",
        "chili" to "🌶️",
        "avocado" to "🥑",
        "lemon" to "🍋",
        "lime" to "🍋",
        "apple" to "🍎",
        "banana" to "🍌",
        "strawberry" to "🍓",
        "grapes" to "🍇",
        "salt" to "🧂",
        "honey" to "🍯",
        "olive oil" to "🫒",
        "oil" to "🫒",
        "water" to "💧",
    )

fun ingredientEmoji(name: String): String? = ingredientEmojis[name.trim().lowercase()]

private val avatarColors: List<Color> =
    listOf(
        Color(0xFFE57373),
        Color(0xFF64B5F6),
        Color(0xFF81C784),
        Color(0xFFFFB74D),
        Color(0xFFBA68C8),
        Color(0xFF4DB6AC),
        Color(0xFFF06292),
        Color(0xFF7986CB),
        Color(0xFFA1887F),
        Color(0xFF9575CD),
    )

fun colorForName(name: String): Color {
    val index = name.lowercase().hashCode().absoluteValue % avatarColors.size
    return avatarColors[index]
}
