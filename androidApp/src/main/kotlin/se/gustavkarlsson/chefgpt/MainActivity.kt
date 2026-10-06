package se.gustavkarlsson.chefgpt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityRetainedScope
import org.koin.core.scope.Scope
import se.gustavkarlsson.chefgpt.di.LocalSessionScope

class MainActivity :
    ComponentActivity(),
    AndroidScopeComponent {
    // Survives configuration changes, closed when the activity finishes (not every onDestroy).
    override val scope: Scope by activityRetainedScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalSessionScope provides scope) {
                App()
            }
        }
    }
}
