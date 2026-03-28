package se.gustavkarlsson.chefgpt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.serializers.MutableStateFlowSerializer
import androidx.savedstate.serialization.serializers.SavedStateSerializer
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.key.Keyer
import coil3.map.Mapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.KoinApplication
import se.gustavkarlsson.chefgpt.api.ImageUrl
import se.gustavkarlsson.chefgpt.api.SessionId
import se.gustavkarlsson.chefgpt.di.AppModule
import se.gustavkarlsson.chefgpt.screens.chat.ChatScreen
import se.gustavkarlsson.chefgpt.screens.start.StartScreen
import se.gustavkarlsson.chefgpt.theme.ChefGptTheme

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Start : Route

    @Serializable
    data class Chat(
        val sessionId: SessionId,
    ) : Route
}

private val routeSerializersModule =
    SerializersModule {
        contextual(SavedStateSerializer)
        contextual(MutableStateFlow::class) { elementSerializers ->
            MutableStateFlowSerializer(elementSerializers.first())
        }

        polymorphic(NavKey::class) {
            subclassesOfSealed<Route>()
        }
    }

@Composable
fun App() {
    KoinApplication(application = { modules(AppModule) }) {
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
        val backStack =
            rememberNavBackStack(
                SavedStateConfiguration { serializersModule = routeSerializersModule },
                Route.Start,
            )

        val navigator = remember(backStack) { Navigator(backStack) }
        CompositionLocalProvider(LocalNavigator provides navigator) {
            ChefGptTheme {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider =
                        entryProvider {
                            entry<Route.Start> { StartScreen() }
                            entry<Route.Chat> { key -> ChatScreen(key.sessionId) }
                        },
                )
            }
        }
    }
}
