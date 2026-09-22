package se.gustavkarlsson.chefgpt

import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera

actual fun deviceSupportsCamera(): Boolean =
    UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceTypeCamera)

actual val devicePlatform: Platform = Platform.Ios
