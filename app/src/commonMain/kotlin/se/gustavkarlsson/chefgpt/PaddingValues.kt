package se.gustavkarlsson.chefgpt

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection

/** Returns a [PaddingValues] whose every edge is the sum of the corresponding edges of [this] and [other]. */
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues = SummedPaddingValues(this, other)

private class SummedPaddingValues(
    private val first: PaddingValues,
    private val second: PaddingValues,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        first.calculateLeftPadding(layoutDirection) + second.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding() = first.calculateTopPadding() + second.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        first.calculateRightPadding(layoutDirection) + second.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding() = first.calculateBottomPadding() + second.calculateBottomPadding()
}
