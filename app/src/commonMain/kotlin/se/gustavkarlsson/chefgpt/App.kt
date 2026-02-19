package se.gustavkarlsson.chefgpt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch

@Composable
fun App() {
    val viewModel = viewModel { FindRecipeViewModel() }
    val viewState by viewModel.viewState.collectAsState()

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                MessageList(
                    messages = viewState.messages,
                    modifier = Modifier.weight(1f),
                )

                MessageInput(
                    userText = viewState.userText,
                    onUserTextChanged = viewState.onUserTextChanged,
                    onClickSend = viewState.onClickSend,
                    onImageAttached = viewState.onImageAttached,
                    onImageCleared = viewState.onImageCleared,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(Int.MAX_VALUE)
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages) { message ->
            MessageBubble(message)
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.subject == Subject.User

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        val sideBasedPadding =
            if (isUser) {
                Modifier.padding(start = 64.dp)
            } else {
                Modifier.padding(end = 64.dp)
            }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color =
                if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(sideBasedPadding)
                    .padding(4.dp),
        ) {
            Markdown(
                content = message.content.text, // TODO what if it sends image?
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun MessageInput(
    userText: String,
    onUserTextChanged: (String) -> Unit,
    onClickSend: (() -> Unit)?,
    onImageAttached: (String) -> Unit,
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
            IconButton(
                onClick = {
                    scope.launch {
                        pickImageFile()?.let { onImageAttached(it) }
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Attach Image",
                )
            }

            IconButton(
                onClick = { onImageCleared?.invoke() },
                enabled = onImageCleared != null,
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Image",
                )
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
