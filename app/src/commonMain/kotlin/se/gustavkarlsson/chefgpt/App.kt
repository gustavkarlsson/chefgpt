package se.gustavkarlsson.chefgpt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.key.Keyer
import coil3.map.Mapper
import kotlinx.io.files.Path
import org.koin.compose.koinInject
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Route
import se.gustavkarlsson.chefgpt.screens.chat.ChatScreen
import se.gustavkarlsson.chefgpt.screens.start.StartScreen
import se.gustavkarlsson.chefgpt.theme.ChefGptTheme

@Composable
fun App() {
    // TODO Extract this
    var initializedImageLoader by rememberSaveable { mutableStateOf(false) }
    if (!initializedImageLoader) {
        setSingletonImageLoaderFactory { context ->
            ImageLoader(context)
                .newBuilder()
                .components {
                    add(Mapper<Path, String> { data, _ -> data.toString() })
                    add(Keyer<Path> { data, _ -> data.toString() })
                    add(Mapper<ImageUrl, String> { data, _ -> data.value })
                    add(Keyer<ImageUrl> { data, _ -> data.value })
                }.build()
        }
        initializedImageLoader = true
    }
    val navigator = koinInject<Navigator>()
    ChefGptTheme {
        // FIXME VM:s do not ge cleared when popping navigator
        NavDisplay(
            backStack = navigator.backStack.collectAsState().value,
            onBack = navigator::pop,
            entryProvider =
                entryProvider {
                    entry<Route.Start> { StartScreen() }
                    entry<Route.Chat> { key -> ChatScreen(key) }
                },
        )
    }
}
