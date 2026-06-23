package se.gustavkarlsson.chefgpt.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// TODO Add logging

class Navigator(
    initialRoute: Route = Route.Start,
) {
    val backStack: StateFlow<List<Route>>
        field = MutableStateFlow(listOf(initialRoute))

    fun push(route: Route) {
        backStack.update { routes -> routes + route }
    }

    fun replaceTop(route: Route) {
        backStack.update { routes -> routes.dropLast(1) + route }
    }

    fun pop() {
        backStack.update { routes -> routes.dropLast(1) }
    }
}
