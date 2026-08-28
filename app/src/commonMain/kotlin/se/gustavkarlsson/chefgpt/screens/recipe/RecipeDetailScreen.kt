package se.gustavkarlsson.chefgpt.screens.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.api.RecipeId
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id
import se.gustavkarlsson.chefgpt.plus
import se.gustavkarlsson.chefgpt.recipes.Ingredient
import se.gustavkarlsson.chefgpt.recipes.Nutrient
import se.gustavkarlsson.chefgpt.recipes.Recipe
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessage
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessageHost
import se.gustavkarlsson.chefgpt.snackbar.rememberSnackbarHostState
import se.gustavkarlsson.chefgpt.theme.LocalMarkdownTypography
import kotlin.time.Duration

private val INGREDIENTS_PANE_MIN_WIDTH = 840.dp
private val NUTRIENTS_PANE_MIN_WIDTH = 1260.dp

@Serializable
@SerialName("recipe_detail")
data class RecipeDetailScreen(
    val sessionId: SessionId,
    val recipeId: RecipeId,
    override val id: Id = Id.new(),
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<RecipeDetailViewModel> { parametersOf(this) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState, viewModel.snackbarMessages)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    uiState: RecipeDetailUiState,
    snackbarMessages: Flow<SnackbarMessage>,
    modifier: Modifier = Modifier,
) {
    val loaded = uiState.content as? RecipeDetailUiState.Content.Loaded
    val snackbarHostState = rememberSnackbarHostState(snackbarMessages)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarMessageHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                title = {
                    val title = loaded?.recipe?.title ?: ""
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = uiState.onClickBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (loaded != null) {
                        IconButton(onClick = loaded.onClickToggleFavorite) {
                            Icon(
                                imageVector =
                                    if (loaded.recipe.favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription =
                                    if (loaded.recipe.favorite) "Remove from favorites" else "Add to favorites",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            val modificationActions = loaded?.modificationActions
            if (modificationActions != null) {
                ModificationBar(modificationActions)
            }
        },
    ) { paddingValues ->
        when (val content = uiState.content) {
            RecipeDetailUiState.Content.Loading -> LoadingContent(paddingValues)
            is RecipeDetailUiState.Content.Error -> ErrorContent(content, paddingValues)
            is RecipeDetailUiState.Content.Loaded -> LoadedContent(content.recipe, paddingValues)
        }
    }
}

// Shown for a recipe that is a modified version of another one, until the user decides
// what should happen to it.
@Composable
private fun ModificationBar(
    actions: RecipeDetailUiState.ModificationActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Edited version of another recipe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { actions.onClickDiscard?.invoke() },
                    enabled = actions.onClickDiscard != null,
                ) {
                    Text("Discard")
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { actions.onClickSaveAsCopy?.invoke() },
                    enabled = actions.onClickSaveAsCopy != null,
                ) {
                    Text("Save as copy")
                }
                Button(
                    onClick = { actions.onClickOverwriteOriginal?.invoke() },
                    enabled = actions.onClickOverwriteOriginal != null,
                ) {
                    Text("Overwrite original")
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = paddingValues + PaddingValues(bottom = 16.dp),
    ) {
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 320.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaceholderBox(widthFraction = 0.65f, height = 28.dp)
                PlaceholderBox(widthFraction = 0.45f, height = 20.dp)
                Spacer(Modifier.height(4.dp))
                PlaceholderBox(widthFraction = 1f, height = 16.dp)
                PlaceholderBox(widthFraction = 0.9f, height = 16.dp)
                PlaceholderBox(widthFraction = 0.95f, height = 16.dp)
            }
        }
        item { SkeletonSectionHeader() }
        items(4) { index ->
            SkeletonStepItem(stepLines = if (index % 2 == 0) 2 else 3)
            if (index < 3) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun PlaceholderBox(
    widthFraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth(widthFraction)
                .height(height)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                ),
    )
}

@Composable
private fun SkeletonSectionHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalDivider()
        Box(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(0.35f)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ),
        )
    }
}

@Composable
private fun SkeletonStepItem(
    stepLines: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp, 28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(stepLines) { lineIndex ->
                PlaceholderBox(
                    widthFraction = if (lineIndex == stepLines - 1) 0.6f else 1f,
                    height = 16.dp,
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    content: RecipeDetailUiState.Content.Error,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Couldn't load recipe",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Something went wrong. Please try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = content.onClickRetry) {
                Text("Try again")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun LoadedContent(
    recipe: Recipe,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxPanes =
            when {
                maxWidth >= NUTRIENTS_PANE_MIN_WIDTH -> 3
                maxWidth >= INGREDIENTS_PANE_MIN_WIDTH -> 2
                else -> 1
            }
        val directive =
            calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
                .copy(
                    maxHorizontalPartitions = maxPanes,
                    defaultPanePreferredWidth = 288.dp,
                )
        val navigator =
            rememberListDetailPaneScaffoldNavigator<Nothing>(
                scaffoldDirective = directive,
                initialDestinationHistory =
                    listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail)),
            )
        val listHidden =
            navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
        val extraHidden =
            navigator.scaffoldValue[ListDetailPaneScaffoldRole.Extra] == PaneAdaptedValue.Hidden

        ListDetailPaneScaffold(
            modifier = Modifier.fillMaxSize(),
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            detailPane = {
                AnimatedPane {
                    MainPane(
                        recipe = recipe,
                        paddingValues = paddingValues,
                        showIngredients = listHidden,
                        showNutrients = extraHidden,
                    )
                }
            },
            listPane = {
                AnimatedPane {
                    SidePane(title = "Ingredients") {
                        if (recipe.ingredients.isEmpty()) {
                            EmptySidePaneText("No ingredients listed")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = paddingValues + PaddingValues(bottom = 16.dp),
                            ) {
                                itemsIndexed(recipe.ingredients) { index, ingredient ->
                                    IngredientItem(ingredient)
                                    if (index < recipe.ingredients.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            extraPane = {
                AnimatedPane {
                    SidePane(title = "Nutrition") {
                        if (recipe.nutrients.isEmpty()) {
                            EmptySidePaneText("No nutritional data available")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = paddingValues + PaddingValues(bottom = 16.dp),
                            ) {
                                itemsIndexed(recipe.nutrients) { index, nutrient ->
                                    NutrientItem(nutrient)
                                    if (index < recipe.nutrients.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainPane(
    recipe: Recipe,
    paddingValues: PaddingValues,
    showIngredients: Boolean,
    showNutrients: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = paddingValues + PaddingValues(bottom = 16.dp),
    ) {
        item { RecipeHero(imageUrl = recipe.imageUrl?.value) }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                val meta = recipe.toMetaChips()
                if (meta.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        meta.forEach { MetaChip(it) }
                    }
                }
            }
        }
        recipe.description?.let { desc ->
            item {
                Markdown(
                    content = desc,
                    typography = LocalMarkdownTypography.current,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        if (showIngredients && recipe.ingredients.isNotEmpty()) {
            item { SectionHeader("Ingredients") }
            itemsIndexed(recipe.ingredients) { index, ingredient ->
                IngredientItem(ingredient)
                if (index < recipe.ingredients.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        if (recipe.steps.isNotEmpty()) {
            item { SectionHeader("Instructions") }
            itemsIndexed(recipe.steps) { index, step ->
                StepItem(index = index, text = step)
                if (index < recipe.steps.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        if (showNutrients && recipe.nutrients.isNotEmpty()) {
            item { SectionHeader("Nutrition") }
            itemsIndexed(recipe.nutrients) { index, nutrient ->
                NutrientItem(nutrient)
                if (index < recipe.nutrients.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun SidePane(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun EmptySidePaneText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(16.dp),
    )
}

@Composable
private fun RecipeHero(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 320.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class MetaChipContent(
    val icon: ImageVector,
    val label: String,
    val value: String,
)

// The total time only says something of its own when neither part of it is known.
private fun Recipe.toMetaChips(): List<MetaChipContent> =
    buildList {
        preparationDuration?.let { add(MetaChipContent(Icons.Default.Timer, "Prep", it.toDisplayString())) }
        cookingDuration?.let { add(MetaChipContent(Icons.Default.Timer, "Cook", it.toDisplayString())) }
        if (preparationDuration == null && cookingDuration == null) {
            duration?.let { add(MetaChipContent(Icons.Default.Timer, "Total", it.toDisplayString())) }
        }
        servings?.let { add(MetaChipContent(Icons.Default.Groups, "Serves", it.toDisplayString())) }
    }

@Composable
private fun MetaChip(
    content: MetaChipContent,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = content.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "${content.label}:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = content.value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun IngredientItem(
    ingredient: Ingredient,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ingredient.amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NutrientItem(
    nutrient: Nutrient,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = nutrient.name,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = nutrient.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun StepItem(
    index: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp).padding(vertical = 4.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Duration.toDisplayString(): String {
    val totalMinutes = inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

private fun IntRange.toDisplayString(): String = if (first == last) "$first" else "$first-$last"
