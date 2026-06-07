package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.ingredients.EmojiAvatar
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.pickImageFile

@Composable
fun IngredientsScreen(route: Route.Ingredients) {
    val viewModel = koinViewModel<IngredientsViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsState()
    Content(uiState)
}

@Composable
private fun Content(uiState: UiState) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = uiState.onClickBack) {
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
            val gridState = rememberLazyGridState()
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.FixedSize(100.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ingredientSection(
                        title = null,
                        ingredients = uiState.inInventory,
                        inInventory = true,
                    )
                    ingredientSection(
                        title = "Previously in store",
                        ingredients = uiState.notInInventory,
                        inInventory = false,
                    )
                }
                IngredientScrollbar(
                    gridState = gridState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }

            IngredientInput(
                modifier = Modifier.fillMaxWidth(),
                input = uiState.input,
            )
        }
    }
}

private fun LazyGridScope.ingredientSection(
    title: String?,
    ingredients: List<UiIngredient>,
    inInventory: Boolean,
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
    items(items = ingredients, key = { it.id.toString() }) { ingredient ->
        IngredientCard(
            modifier = Modifier.animateItem(),
            ingredient = ingredient,
            inInventory = inInventory,
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: UiIngredient,
    inInventory: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .size(100.dp)
                .clickable { ingredient.onClick(ingredient.id) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(if (inInventory) 1f else 0.5f)
                        .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmojiAvatar(ingredient.icon)
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (ingredient.onClickDestroy != null) {
                IconButton(
                    onClick = { ingredient.onClickDestroy(ingredient.id) },
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
private fun IngredientInput(
    input: UiInput,
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
                value = input.text,
                onValueChange = input.onTextChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.isShiftPressed) {
                                    false
                                } else {
                                    input.onClickAdd?.invoke()
                                    true
                                }
                            } else {
                                false
                            }
                        },
                placeholder = { Text("Add an ingredient...") },
                singleLine = true,
            )
            val scope = rememberCoroutineScope()
            if (input.onScanImageSelected == null) {
                // Scanning can take a while; show progress in place of the camera button.
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                IconButton(
                    onClick = {
                        scope.launch {
                            pickImageFile()?.let(input.onScanImageSelected)
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan ingredients from image",
                    )
                }
            }
            IconButton(
                onClick = { input.onClickAdd?.invoke() },
                enabled = input.onClickAdd != null,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Add",
                )
            }
        }
    }
}
