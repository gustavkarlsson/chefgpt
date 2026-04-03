package se.gustavkarlsson.chefgpt.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.api.ApiAgentEvent
import se.gustavkarlsson.chefgpt.api.ApiAgentMessage
import se.gustavkarlsson.chefgpt.api.ApiAgentReasoning
import se.gustavkarlsson.chefgpt.api.ApiEvent
import se.gustavkarlsson.chefgpt.api.ApiSystemEvent
import se.gustavkarlsson.chefgpt.api.ApiUserEvent
import se.gustavkarlsson.chefgpt.api.ApiUserMessage
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.pickImageFile
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel.ViewState

@Composable
fun ChatScreen(chat: Route.Chat) {
    val viewModel = koinViewModel<ChatViewModel> { parametersOf(chat) }
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
                Surface(
                    color = if (viewState.connected) Color.Green else Color.Red,
                    shape = CircleShape,
                    modifier = Modifier.size(16.dp),
                ) {
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            MessageList(
                events = viewState.events,
                modifier = Modifier.weight(1f),
            )

            MessageInput(
                userText = viewState.userText,
                attachedImage = viewState.attachedImage,
                onUserTextChanged = viewState.onUserTextChanged,
                onClickSend = viewState.onClickSend,
                onImageAttached = viewState.onImageAttached,
                onImageCleared = viewState.onImageCleared,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MessageList(
    events: List<ApiEvent>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.animateScrollToItem(Int.MAX_VALUE)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        itemsIndexed(events) { index, event ->
            if (index == 0) {
                Spacer(Modifier.height(8.dp))
            }
            MessageBubble(event)
            Spacer(Modifier.height(8.dp))
        }
    }
}

// TODO Handle different types of actions more exhaustively
@Composable
private fun MessageBubble(event: ApiEvent) {
    val isUser =
        when (event) {
            is ApiSystemEvent -> return
            is ApiAgentEvent -> false
            is ApiUserEvent -> true
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier =
                Modifier
                    .align(
                        if (isUser) {
                            Alignment.CenterEnd
                        } else {
                            Alignment.CenterStart
                        },
                    ).widthIn(max = 400.dp)
                    .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            color =
                if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
        ) {
            Column {
                if (event is ApiAgentReasoning) {
                    Text("Reasoning", style = MaterialTheme.typography.bodyMedium)
                }
                if (event is ApiUserMessage) {
                    event.imageUrl?.let { image ->
                        AsyncImage(
                            model = image,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .align(
                                        if (isUser) {
                                            Alignment.End
                                        } else {
                                            Alignment.Start
                                        },
                                    ).fillMaxWidth()
                                    .heightIn(max = 300.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                val text =
                    when (event) {
                        is ApiAgentMessage -> event.text
                        is ApiAgentReasoning -> event.text
                        is ApiUserMessage -> event.text
                    }
                text?.let {
                    Markdown(
                        content = it,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    userText: String,
    attachedImage: Path?,
    onUserTextChanged: (String) -> Unit,
    onClickSend: (() -> Unit)?,
    onImageAttached: (Path) -> Unit,
    onImageCleared: (() -> Unit)?,
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
                value = userText,
                onValueChange = onUserTextChanged,
                modifier =
                    Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.isShiftPressed) {
                                    false // Allow shift+enter to insert newline
                                } else {
                                    if (onClickSend != null && userText.isNotBlank()) {
                                        onClickSend.invoke()
                                    }
                                    true
                                }
                            } else {
                                false
                            }
                        },
                placeholder = { Text("Type a message...") },
            )

            val scope = rememberCoroutineScope()
            if (attachedImage == null) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pickImageFile()?.let { onImageAttached(it) }
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
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .hoverable(interactionSource = interactionSource)
                            .clickable { onImageCleared?.invoke() },
                ) {
                    AsyncImage(
                        model = attachedImage,
                        contentDescription = "Attached Image",
                        modifier =
                            Modifier
                                .matchParentSize(),
                        contentScale = ContentScale.Crop,
                    )
                    if (isHovered) {
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Image",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { onClickSend?.invoke() },
                enabled = onClickSend != null && userText.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                )
            }
        }
    }
}
