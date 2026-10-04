package se.gustavkarlsson.chefgpt

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceConfigTest {
    @Test
    fun `supportsAnyPhotoSource is true when only the camera is supported`() {
        val config = DeviceConfig(Platform.Android, supportsCamera = true, supportsFilePicker = false)

        val result = config.supportsAnyPhotoSource

        assertTrue(result)
    }

    @Test
    fun `supportsAnyPhotoSource is true when only the file picker is supported`() {
        val config = DeviceConfig(Platform.Desktop, supportsCamera = false, supportsFilePicker = true)

        val result = config.supportsAnyPhotoSource

        assertTrue(result)
    }

    @Test
    fun `supportsAnyPhotoSource is true when both sources are supported`() {
        val config = DeviceConfig(Platform.Android, supportsCamera = true, supportsFilePicker = true)

        val result = config.supportsAnyPhotoSource

        assertTrue(result)
    }

    @Test
    fun `supportsAnyPhotoSource is false when neither source is supported`() {
        val config = DeviceConfig(Platform.Web, supportsCamera = false, supportsFilePicker = false)

        val result = config.supportsAnyPhotoSource

        assertFalse(result)
    }
}
