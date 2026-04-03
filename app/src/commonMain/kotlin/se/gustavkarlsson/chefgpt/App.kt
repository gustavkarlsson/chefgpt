package se.gustavkarlsson.chefgpt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.key.Keyer
import coil3.map.Mapper
import kotlinx.io.files.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import se.gustavkarlsson.chefgpt.api.ApiChat
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.chat.ChatScreen
import se.gustavkarlsson.chefgpt.screens.start.StartScreen
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.theme.ChefGptTheme

@Serializable
@SerialName("route")
sealed interface Route : NavKey {
    @Serializable
    @SerialName("start")
    data object Start : Route

    @Serializable
    @SerialName("chat")
    data class Chat(
        val sessionId: SessionId,
        val chat: ApiChat,
    ) : Route
}

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
        NavDisplay(
            backStack = navigator.backStack,
            onBack = { navigator.pop() },
            entryProvider =
                entryProvider {
                    entry<Route.Start> { StartScreen() }
                    entry<Route.Chat> { key -> ChatScreen(key) }
                },
        )
    }
}
