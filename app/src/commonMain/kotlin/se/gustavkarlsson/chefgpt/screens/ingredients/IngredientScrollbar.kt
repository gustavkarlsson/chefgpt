package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// TODO Consider dropping or reworking scrollbar

/**
 * A vertical scrollbar for [gridState]. Rendered only on platforms with pointer-driven scrolling (desktop);
 * a no-op elsewhere.
 */
@Composable
expect fun IngredientScrollbar(
    gridState: LazyGridState,
    modifier: Modifier,
)
