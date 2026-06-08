package se.gustavkarlsson.chefgpt.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import se.gustavkarlsson.chefgpt.screens.start.StartScreen

// TODO Add logging

class Navigator(
    initialScreen: Screen = StartScreen(),
) {
    private val _backStack = MutableStateFlow(listOf(initialScreen))
    val backStack: StateFlow<List<Screen>> = _backStack.asStateFlow()

    fun push(screen: Screen) {
        _backStack.update { routes -> routes + screen }
    }

    fun replaceTop(screen: Screen) {
        _backStack.update { routes -> routes.dropLast(1) + screen }
    }

    fun pop() {
        _backStack.update { routes -> routes.dropLast(1) }
    }
}
