package se.gustavkarlsson.chefgpt.di

actual class NativeComponent {
    actual fun getInfo(): String = Runtime.version().toString()
}
