package se.gustavkarlsson.chefgpt.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownTypography

/**
 * Markdown typography for the current theme, provided at the root of the Compose
 * tree by [ChefGptTheme] so every markdown surface shares a single instance.
 */
val LocalMarkdownTypography =
    staticCompositionLocalOf<MarkdownTypography> {
        error("No MarkdownTypography provided")
    }

/**
 * The default markdown headings render far larger than surrounding body text.
 * This halves the largest heading and remaps the rest proportionally so they sit
 * between the body text size and the new largest heading size.
 *
 * The scaled heading styles are cached per theme so the whole tree shares a single
 * instance instead of reallocating on each recomposition.
 */
@Composable
fun rememberScaledMarkdownTypography(): MarkdownTypography {
    val typography = MaterialTheme.typography
    val headings =
        remember(typography) {
            val bodySize = typography.bodyLarge.fontSize.value
            val largestSize = typography.displayLarge.fontSize.value
            val targetLargestSize = largestSize / 2f

            fun TextStyle.scaledHeading(): TextStyle {
                val current = fontSize.value
                val fraction = (current - bodySize) / (largestSize - bodySize)
                val factor = (bodySize + fraction * (targetLargestSize - bodySize)) / current
                return copy(
                    fontSize = (current * factor).sp,
                    lineHeight = if (lineHeight.isSpecified) (lineHeight.value * factor).sp else lineHeight,
                )
            }

            listOf(
                typography.displayLarge.scaledHeading(),
                typography.displayMedium.scaledHeading(),
                typography.displaySmall.scaledHeading(),
                typography.headlineMedium.scaledHeading(),
                typography.headlineSmall.scaledHeading(),
                typography.titleLarge.scaledHeading(),
            )
        }

    return markdownTypography(
        h1 = headings[0],
        h2 = headings[1],
        h3 = headings[2],
        h4 = headings[3],
        h5 = headings[4],
        h6 = headings[5],
    )
}
