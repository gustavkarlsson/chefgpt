package se.gustavkarlsson.chefgpt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.key.Keyer
import coil3.map.Mapper
import kotlinx.io.files.Path
import org.koin.compose.koinInject
import org.koin.compose.scope.UnboundKoinScope
import org.koin.core.annotation.KoinDelicateAPI
import org.koin.core.annotation.KoinExperimentalAPI
import se.gustavkarlsson.chefgpt.api.common.ImageUrl
import se.gustavkarlsson.chefgpt.debug.DebugBubbleOverlay
import se.gustavkarlsson.chefgpt.debug.shouldShowDebugBubble
import se.gustavkarlsson.chefgpt.di.LocalActivityRetainedScope
import se.gustavkarlsson.chefgpt.navigation.BottomSheetSceneStrategy
import se.gustavkarlsson.chefgpt.navigation.NavigationTransitions
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.screens.debug.DebugScreen
import se.gustavkarlsson.chefgpt.screens.updaterequired.UpdateRequiredScreen
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager
import se.gustavkarlsson.chefgpt.snackbar.SnackbarMessageHost
import se.gustavkarlsson.chefgpt.snackbar.rememberSnackbarHostState
import se.gustavkarlsson.chefgpt.theme.ChefGptTheme
import se.gustavkarlsson.chefgpt.updates.UpdateRequiredNotifier

@OptIn(KoinDelicateAPI::class, KoinExperimentalAPI::class)
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
    // Snackbar messages are app-wide, so the manager is a Koin singleton injected from
    // the root context rather than the activity retained scope below.
    val snackbarManager = koinInject<SnackbarManager>()
    val snackbarHostState = rememberSnackbarHostState(snackbarManager.messages)
    // The scope retained bindings resolve against: the activity's retained scope on
    // Android — which survives configuration changes and is recreated after the activity
    // truly finishes — and a process-lifetime scope on the other platforms.
    UnboundKoinScope(LocalActivityRetainedScope.current) {
        val navigator = koinInject<Navigator>()
        val updateRequiredNotifier = koinInject<UpdateRequiredNotifier>()
        val backStack = navigator.backStack.collectAsState().value
        LaunchedEffect(updateRequiredNotifier) {
            updateRequiredNotifier.updateRequired.collect { updateRequired ->
                if (updateRequired) {
                    navigator.replaceAll(UpdateRequiredScreen())
                }
            }
        }
        ChefGptTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = navigator::pop,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    sceneStrategies = listOf(BottomSheetSceneStrategy()),
                    transitionSpec = NavigationTransitions.transitionSpec,
                    popTransitionSpec = NavigationTransitions.popTransitionSpec,
                    predictivePopTransitionSpec = NavigationTransitions.predictivePopTransitionSpec,
                    entryProvider = { screen ->
                        NavEntry(
                            key = screen,
                            contentKey = screen.id.value,
                            metadata =
                                if (screen is Screen.BottomSheet) {
                                    BottomSheetSceneStrategy.bottomSheetMetadata()
                                } else {
                                    emptyMap()
                                },
                        ) { screen.Content() }
                    },
                )
                SnackbarMessageHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding(),
                )
                if (IS_DEBUG_BUILD) {
                    DebugBubbleOverlay(
                        visible = shouldShowDebugBubble(backStack),
                        onClick = { navigator.push(DebugScreen()) },
                    )
                }
            }
        }
    }
}
