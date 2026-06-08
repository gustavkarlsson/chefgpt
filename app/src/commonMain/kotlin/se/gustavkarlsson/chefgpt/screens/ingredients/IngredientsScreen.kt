package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.ingredients.EmojiAvatar
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.pickImageFile
import se.gustavkarlsson.chefgpt.plus
import androidx.compose.foundation.lazy.items as lazyItems

@Composable
fun IngredientsScreen(route: Route.Ingredients) {
    val viewModel = koinViewModel<IngredientsViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Content(uiState)
}

@Composable
private fun Content(
    uiState: UiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier =
                    Modifier
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        ).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = uiState.onClickBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Ingredients",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
        bottomBar = {
            IngredientInput(
                modifier = Modifier.fillMaxWidth(),
                input = uiState.input,
            )
        },
    ) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier.fillMaxWidth(),
            columns = GridCells.FixedSize(100.dp),
            contentPadding = paddingValues + PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ingredientSection(
                title = null,
                ingredients = uiState.inInventory,
            )
            ingredientSection(
                title = "Previously in store",
                ingredients = uiState.notInInventory,
            )
        }
    }
}

private fun LazyGridScope.ingredientSection(
    title: String?,
    ingredients: List<UiIngredient>,
) {
    if (ingredients.isEmpty()) return
    if (title != null) {
        stickyHeader(key = title) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(items = ingredients, key = { it.key }) { ingredient ->
        IngredientCard(
            modifier = Modifier.animateItem(),
            ingredient = ingredient,
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: UiIngredient,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(100.dp).clickable { ingredient.onClick(ingredient.key) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(if (ingredient.dimmed) 0.5f else 1f)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmojiAvatar(ingredient.icon)
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            if (ingredient.onClickDestroy != null) {
                IconButton(
                    modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                    onClick = { ingredient.onClickDestroy(ingredient.key) },
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientSuggestions(
    suggestions: List<UiIngredient>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // Hold onto the last non-empty list so the panel keeps rendering its cards while it animates out.
    var lastSuggestions by remember { mutableStateOf(suggestions) }
    if (suggestions.isNotEmpty()) lastSuggestions = suggestions
    val animationSpec =
        spring(
            stiffness = Spring.StiffnessHigh,
            visibilityThreshold = IntSize.VisibilityThreshold,
        )
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        modifier = modifier,
        enter = expandVertically(animationSpec, expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(animationSpec, shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lazyItems(items = lastSuggestions, key = { it.key }) { suggestion ->
                IngredientCard(
                    modifier = Modifier.animateItem(),
                    ingredient = suggestion,
                )
            }
        }
    }
}

@Composable
private fun IngredientInput(
    input: UiInput,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ).padding(bottom = 16.dp, top = 4.dp),
        ) {
            IngredientSuggestions(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                contentPadding =
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal)
                        .asPaddingValues() + PaddingValues(horizontal = 16.dp),
                suggestions = input.suggestions,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = input.text,
                    onValueChange = input.onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add an ingredient...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { input.onClickAdd?.invoke() }),
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
}
