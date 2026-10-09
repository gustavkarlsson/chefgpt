package se.gustavkarlsson.chefgpt.debug

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.screens.debug.DebugScreen
import kotlin.math.roundToInt

private val BubbleSize = 48.dp

private val BubbleMargin = 8.dp

// Hides the bubble while a modal is on top (bottom sheet or the debug screen itself),
// so it neither floats over the scrim nor stacks a second debug screen when tapped.
internal fun shouldShowDebugBubble(backStack: List<Screen>): Boolean {
    val top = backStack.lastOrNull() ?: return false
    return top !is DebugScreen && top !is Screen.BottomSheet
}

// Clamps an offset so the bubble stays fully inside the available area.
internal fun Offset.clampedToBounds(maxOffset: Offset): Offset =
    Offset(
        x = x.coerceIn(0f, maxOf(0f, maxOffset.x)),
        y = y.coerceIn(0f, maxOf(0f, maxOffset.y)),
    )

@Composable
internal fun DebugBubbleOverlay(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val density = LocalDensity.current
        val bubbleSizePx = with(density) { BubbleSize.toPx() }
        val marginPx = with(density) { BubbleMargin.toPx() }
        var position by remember {
            mutableStateOf(
                Offset(
                    x = constraints.maxWidth - bubbleSizePx - marginPx,
                    y = marginPx,
                ),
            )
        }
        val maxOffset =
            Offset(
                x = constraints.maxWidth - bubbleSizePx,
                y = constraints.maxHeight - bubbleSizePx,
            )
        LaunchedEffect(maxOffset) {
            position = position.clampedToBounds(maxOffset)
        }
        if (visible) {
            Surface(
                modifier =
                    Modifier
                        .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                        .size(BubbleSize)
                        .pointerInput(constraints.maxWidth, constraints.maxHeight) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                position = (position + dragAmount).clampedToBounds(maxOffset)
                            }
                        },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
            ) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Debug",
                    )
                }
            }
        }
    }
}
