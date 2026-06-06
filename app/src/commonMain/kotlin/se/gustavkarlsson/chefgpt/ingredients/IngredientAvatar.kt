package se.gustavkarlsson.chefgpt.ingredients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import org.kodein.emoji.Emoji as KodeinEmoji

/**
 * What an [EmojiAvatar] should display: either a resolved [Emoji], or a single [Character] (on a colored circle)
 * as a fallback.
 */
sealed interface EmojiAvatarModel {
    data class Emoji(
        val emoji: KodeinEmoji,
    ) : EmojiAvatarModel

    data class Character(
        val character: Char,
        val color: Color,
    ) : EmojiAvatarModel

    companion object {
        /**
         * Builds a model for [name], preferring [emoji] and otherwise falling back to a colored avatar based on
         * the first character of [name].
         */
        fun of(
            emoji: KodeinEmoji?,
            name: String,
        ): EmojiAvatarModel =
            if (emoji != null) {
                Emoji(emoji)
            } else {
                Character(name.first().uppercaseChar(), colorForName(name))
            }
    }
}

/**
 * Visual representation of an [EmojiAvatarModel]: the emoji glyph, or a colored circular avatar with a letter.
 */
@Composable
fun EmojiAvatar(
    model: EmojiAvatarModel,
    modifier: Modifier = Modifier,
) {
    when (model) {
        is EmojiAvatarModel.Emoji -> {
            Text(
                text = model.emoji.details.string,
                style = MaterialTheme.typography.headlineLarge,
                modifier = modifier,
            )
        }

        is EmojiAvatarModel.Character -> {
            Box(
                modifier =
                    modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(model.color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = model.character.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
        }
    }
}

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

private fun colorForName(name: String): Color {
    val index = name.lowercase().hashCode().absoluteValue % avatarColors.size
    return avatarColors[index]
}
