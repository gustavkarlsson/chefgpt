package se.gustavkarlsson.chefgpt

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import se.gustavkarlsson.chefgpt.di.initKoin

class ChefGptApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        initKoin()
    }

    companion object {
        // Set once from onCreate before Koin is initialized.
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }
}
