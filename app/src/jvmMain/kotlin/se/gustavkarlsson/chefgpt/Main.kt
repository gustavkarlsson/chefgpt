package se.gustavkarlsson.chefgpt

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import se.gustavkarlsson.chefgpt.di.initKoin

fun main() {
    initKoin()
    application(exitProcessOnExit = true) {
        Window(::exitApplication, title = "ChefGPT") {
            App()
        }
    }
}
