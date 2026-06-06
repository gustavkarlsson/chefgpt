package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun IngredientScrollbar(
    gridState: LazyGridState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(gridState),
        modifier = modifier,
    )
}
