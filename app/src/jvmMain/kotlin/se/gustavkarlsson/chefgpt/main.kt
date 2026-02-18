package se.gustavkarlsson.chefgpt

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application(exitProcessOnExit = true) {
    Window(::exitApplication, title = "ChefGPT") {
        App()
    }
}
