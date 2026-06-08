package se.gustavkarlsson.chefgpt.screens.ingredients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.ingredients.EmojiAvatar
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.pickImageFile
import se.gustavkarlsson.chefgpt.plus
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessage
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessageHost
import se.gustavkarlsson.chefgpt.snackbar.rememberSnackbarHostState

@Composable
fun IngredientsScreen(route: Route.Ingredients) {
    val viewModel = koinViewModel<IngredientsViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Content(uiState, viewModel.snackbarMessages)
}

@Composable
private fun Content(
    uiState: UiState,
    snackbarMessages: Flow<SnackbarMessage>,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessages)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarMessageHost(snackbarHostState) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
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
                title = uiState.secondSection.title,
                ingredients = uiState.secondSection.ingredients,
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
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            if (ingredient.isNew) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
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
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(input.autoFocus) {
                if (input.autoFocus) focusRequester.requestFocus()
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = input.text,
                    onValueChange = input.onTextChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text("Add an ingredient...") },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
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
