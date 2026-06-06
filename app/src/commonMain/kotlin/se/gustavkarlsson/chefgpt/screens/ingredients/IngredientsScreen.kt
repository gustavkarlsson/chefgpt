package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsViewModel.Ingredient
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsViewModel.ViewState

@Composable
fun IngredientsScreen(route: Route.Ingredients) {
    val viewModel = koinViewModel<IngredientsViewModel> { parametersOf(route) }
    val viewState by viewModel.viewState.collectAsState()
    Content(viewState)
}

@Composable
private fun Content(viewState: ViewState) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewState.onClickBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            LazyVerticalGrid(
                columns = GridCells.FixedSize(100.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ingredientSection(
                    title = null,
                    ingredients = viewState.inStore,
                    onClickIngredient = viewState.onClickIngredient,
                )
                ingredientSection(
                    title = "Previously in store",
                    ingredients = viewState.previouslyInStore,
                    onClickIngredient = viewState.onClickIngredient,
                    onDestroyIngredient = viewState.onDestroyIngredient,
                )
            }

            IngredientInput(
                inputText = viewState.inputText,
                onInputChange = viewState.onInputChange,
                onClickAdd = viewState.onClickAdd,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyGridScope.ingredientSection(
    title: String?,
    ingredients: List<Ingredient>,
    onClickIngredient: (Ingredient) -> Unit,
    onDestroyIngredient: ((Ingredient) -> Unit)? = null,
) {
    if (ingredients.isEmpty()) return
    if (title != null) {
        stickyHeader(key = title) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
    items(items = ingredients, key = { it.id }) { ingredient ->
        IngredientCard(
            ingredient = ingredient,
            onClick = { onClickIngredient(ingredient) },
            onDestroy = onDestroyIngredient?.let { { it(ingredient) } },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: Ingredient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDestroy: (() -> Unit)? = null,
) {
    Surface(
        modifier =
            modifier
                .size(100.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(if (ingredient.inInventory) 1f else 0.5f)
                        .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IngredientImage(ingredient.name)
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (onDestroy != null) {
                IconButton(
                    onClick = onDestroy,
                    modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientImage(name: String) {
    val emoji = ingredientEmoji(name)
    if (emoji != null) {
        Text(text = emoji, style = MaterialTheme.typography.headlineLarge)
    } else {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colorForName(name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun IngredientInput(
    inputText: String,
    onInputChange: (String) -> Unit,
    onClickAdd: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.isShiftPressed) {
                                    false
                                } else {
                                    onClickAdd?.invoke()
                                    true
                                }
                            } else {
                                false
                            }
                        },
                placeholder = { Text("Add an ingredient...") },
                singleLine = true,
            )
            IconButton(
                onClick = { onClickAdd?.invoke() },
                enabled = onClickAdd != null,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Add",
                )
            }
        }
    }
}
