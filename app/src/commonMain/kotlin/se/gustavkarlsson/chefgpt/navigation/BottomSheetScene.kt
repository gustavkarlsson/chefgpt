package se.gustavkarlsson.chefgpt.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** An [OverlayScene] that renders its [entry] in a Material 3 [ModalBottomSheet]. */
internal class BottomSheetScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    @OptIn(ExperimentalMaterial3Api::class)
    override val content: @Composable () -> Unit = {
        ModalBottomSheet(onDismissRequest = onBack) {
            entry.Content()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BottomSheetScene<*>

        return key == other.key &&
            entry == other.entry &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries
    }

    override fun hashCode(): Int =
        key.hashCode() * 31 +
            entry.hashCode() * 31 +
            previousEntries.hashCode() * 31 +
            overlaidEntries.hashCode() * 31

    override fun toString(): String =
        "BottomSheetScene(key=$key, entry=$entry, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries)"
}

/**
 * Renders the top entry as a [BottomSheetScene] when it is marked as a bottom sheet
 * (see [Screen.BottomSheet]). Registered in [androidx.navigation3.ui.NavDisplay]'s
 * `sceneStrategies`; the single-pane fallback renders everything else.
 */
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        if (lastEntry.metadata[BOTTOM_SHEET_KEY] == null) return null
        return BottomSheetScene(
            key = lastEntry.contentKey,
            entry = lastEntry,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            onBack = onBack,
        )
    }

    companion object {
        const val BOTTOM_SHEET_KEY = "bottom-sheet"

        fun bottomSheetMetadata(): Map<String, Any> = mapOf(BOTTOM_SHEET_KEY to Unit)
    }
}
