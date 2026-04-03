package se.gustavkarlsson.chefgpt.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class Navigator(
    initialRoute: Route = Route.Start,
) {
    private val _backStack = MutableStateFlow<List<Route>>(listOf(initialRoute))
    val backStack: StateFlow<List<Route>> = _backStack.asStateFlow()

    fun push(route: Route) {
        _backStack.update { routes -> routes + route }
    }

    fun replaceTop(route: Route) {
        _backStack.update { routes -> routes.dropLast(1) + route }
    }

    fun pop() {
        _backStack.update { routes -> routes.dropLast(1) }
    }
}
