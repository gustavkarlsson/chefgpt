package se.gustavkarlsson.chefgpt.di

import platform.UIKit.UIDevice

actual class NativeComponent {
    actual fun getInfo(): String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
