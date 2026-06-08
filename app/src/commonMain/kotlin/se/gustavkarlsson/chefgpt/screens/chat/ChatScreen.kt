package se.gustavkarlsson.chefgpt.screens.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.ingredients.EmojiAvatar
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.pickImageFile
import se.gustavkarlsson.chefgpt.plus
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChatScreen(route: Route.Chat) {
    val viewModel = koinViewModel<ChatViewModel> { parametersOf(route) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Content(uiState, viewModel.ingredientChanges)
}

@Composable
private fun Content(
    uiState: UiState,
    ingredientChanges: Flow<IngredientChange>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                    ConnectionIndicator(connected = uiState.connected)
                    Text(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        text = uiState.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IngredientButton(
                        ingredientChanges = ingredientChanges,
                        onClick = uiState.onClickIngredients,
                    )
                }
            }
        },
        bottomBar = {
            MessageInput(
                modifier = Modifier.fillMaxWidth(),
                input = uiState.input,
            )
        },
    ) { paddingValues ->
        when (val content = uiState.content) {
            is UiContent.Empty -> {
                EmptyState(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    content = content,
                )
            }

            is UiContent.Messages -> {
                MessageList(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = paddingValues,
                    messages = content.messages,
                )
            }
        }
    }
}

@Composable
private fun ConnectionIndicator(
    connected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(16.dp),
        color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        shape = CircleShape,
    ) {}
}

// How long an ingredient avatar travels onto/off the button, plus the fade applied at each end of that travel
// (kept well under half the travel so it reads as a quick fade-in then fade-out while moving), and the pause
// between queued changes. Avatars start/end this far toward the start edge (RTL-aware) of the button.
private val TRAVEL = 500.milliseconds
private val FADE = 140.milliseconds
private val CHANGE_GAP = 100.milliseconds
private val TRAVEL_DISTANCE = 48.dp

@Composable
private fun IngredientButton(
    ingredientChanges: Flow<IngredientChange>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The queue buffers changes so they animate one at a time, even when several
    // ingredients are added or removed in quick succession.
    val queue = remember { Channel<IngredientChange>(Channel.UNLIMITED) }
    LaunchedEffect(ingredientChanges) {
        ingredientChanges.collect { queue.send(it) }
    }

    var current by remember { mutableStateOf<IngredientChange?>(null) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(queue) {
        for (change in queue) {
            current = change
            progress.snapTo(0f)
            progress.animateTo(1f, tween(TRAVEL.inWholeMilliseconds.toInt(), easing = LinearEasing))
            delay(CHANGE_GAP)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        current?.let { change ->
            val p = progress.value
            // Added travels onto the button (start edge -> center); removed travels off it (center -> start edge).
            val (from, to) =
                when (change) {
                    is IngredientChange.Added -> -TRAVEL_DISTANCE to 0.dp
                    is IngredientChange.Removed -> 0.dp to -TRAVEL_DISTANCE
                }
            val fade = (FADE / TRAVEL).toFloat()
            val alpha =
                when {
                    p < fade -> p / fade
                    p > 1f - fade -> (1f - p) / fade
                    else -> 1f
                }.coerceIn(0f, 1f)
            EmojiAvatar(
                model = change.icon,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .offset(x = lerp(from, to, p))
                        .alpha(alpha),
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Kitchen,
                contentDescription = "Ingredients",
            )
        }
    }
}

@Composable
private fun EmptyState(
    content: UiContent.Empty,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = content.headline,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = content.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = Modifier.padding(top = 24.dp),
            onClick = { content.onClickPrompt?.invoke(content.examplePrompt) },
            enabled = content.onClickPrompt != null,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                text = content.examplePrompt,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<UiMessage>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val listState = rememberLazyListState()

    // reverseLayout anchors the list at the bottom, so the newest message (index 0
    // of the reversed list) stays pinned there regardless of its height.
    val reversedMessages = remember(messages) { messages.asReversed() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = contentPadding + PaddingValues(horizontal = 8.dp, vertical = 16.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = reversedMessages, key = { it.id }) { message ->
            MessageBubble(message = message)
        }
    }
}

@Composable
private fun MessageBubble(
    message: UiMessage,
    modifier: Modifier = Modifier,
) {
    val fromUser = message is UiMessage.User
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier =
                Modifier
                    .align(if (fromUser) Alignment.CenterEnd else Alignment.CenterStart)
                    .widthIn(max = minOf(400.dp, maxWidth * 0.8f))
                    .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            color =
                if (fromUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
        ) {
            Column {
                when (message) {
                    is UiMessage.User -> {
                        message.imageUrl?.let { imageUrl ->
                            AsyncImage(
                                modifier =
                                    Modifier
                                        .align(Alignment.End)
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp),
                                model = imageUrl,
                                contentDescription = "Attached image",
                                contentScale = ContentScale.Crop,
                            )
                        }
                        message.text?.let { MessageText(it) }
                    }

                    is UiMessage.Agent -> {
                        if (message.reasoning) {
                            Text("Reasoning", style = MaterialTheme.typography.bodyMedium)
                        }
                        MessageText(message.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = text,
        modifier = modifier.padding(12.dp),
    )
}

@Composable
private fun MessageInput(
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
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            TextField(
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.isShiftPressed) {
                                    false // Allow shift+enter to insert newline
                                } else {
                                    input.onClickSend?.invoke()
                                    true
                                }
                            } else {
                                false
                            }
                        },
                value = input.text,
                onValueChange = input.onTextChanged,
                placeholder = { Text("Type a message...") },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
            )

            AttachedImageButton(
                attachedImage = input.attachedImage,
                onImageAttached = input.onImageAttached,
                onClickClearImage = input.onClickClearImage,
            )

            IconButton(
                onClick = { input.onClickSend?.invoke() },
                enabled = input.onClickSend != null,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                )
            }
        }
    }
}

@Composable
private fun AttachedImageButton(
    attachedImage: Path?,
    onImageAttached: (Path) -> Unit,
    onClickClearImage: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    if (attachedImage == null) {
        IconButton(
            modifier = modifier,
            onClick = {
                scope.launch {
                    pickImageFile()?.let(onImageAttached)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Take photo",
            )
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        Box(
            modifier =
                modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .hoverable(interactionSource = interactionSource)
                    .clickable { onClickClearImage?.invoke() },
        ) {
            AsyncImage(
                modifier = Modifier.matchParentSize(),
                model = attachedImage,
                contentDescription = "Attached image",
                contentScale = ContentScale.Crop,
            )
            if (isHovered) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear image",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}
