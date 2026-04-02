package se.gustavkarlsson.chefgpt

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey

class Navigator(
    initialRoute: Route = Route.Start,
) {
    private val _backStack = mutableStateListOf<NavKey>(initialRoute)
    val backStack: List<NavKey> = _backStack

    fun push(route: Route) {
        _backStack.add(route)
    }

    fun replace(route: Route) {
        _backStack.removeLastOrNull()
        _backStack.add(route)
    }

    fun pop() {
        _backStack.removeLastOrNull()
    }
}
