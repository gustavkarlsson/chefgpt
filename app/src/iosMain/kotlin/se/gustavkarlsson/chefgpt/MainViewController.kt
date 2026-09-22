package se.gustavkarlsson.chefgpt

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController
import se.gustavkarlsson.chefgpt.di.initKoin

// Android starts Koin from its Application and desktop from main(). iOS has no such hook,
// so the entry point does it, guarded because SwiftUI may recreate the hosting controller.
fun MainViewController(): UIViewController {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        initKoin()
    }
    return ComposeUIViewController { App() }
}
