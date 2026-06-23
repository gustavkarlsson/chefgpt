package se.gustavkarlsson.chefgpt.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import se.gustavkarlsson.chefgpt.screens.start.StartScreen

// TODO Add logging

class Navigator(
    initialScreen: Screen = StartScreen(),
) {
    val backStack: StateFlow<List<Screen>>
        field = MutableStateFlow(listOf(initialScreen))

    fun push(screen: Screen) {
        backStack.update { routes -> routes + screen }
    }

    fun replaceTop(screen: Screen) {
        backStack.update { routes -> routes.dropLast(1) + screen }
    }

    fun pop() {
        backStack.update { routes -> routes.dropLast(1) }
    }
}
