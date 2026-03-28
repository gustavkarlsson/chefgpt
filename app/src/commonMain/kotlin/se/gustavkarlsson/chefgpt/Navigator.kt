package se.gustavkarlsson.chefgpt

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey

class Navigator(
    private val backStack: MutableList<NavKey>,
) {
    fun push(route: Route) {
        backStack.add(route)
    }

    fun replace(route: Route) {
        backStack.removeLastOrNull()
        backStack.add(route)
    }

    fun pop() {
        backStack.removeLastOrNull()
    }
}

val LocalNavigator = compositionLocalOf<Navigator> { error("No Navigator provided") }
