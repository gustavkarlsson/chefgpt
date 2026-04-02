package se.gustavkarlsson.chefgpt.di

actual class NativeComponent {
    actual fun getInfo(): String = "Android ${android.os.Build.VERSION.SDK_INT}"
}
