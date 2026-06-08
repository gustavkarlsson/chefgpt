package se.gustavkarlsson.chefgpt.navigation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.random.Random

interface Screen : NavKey {
    val id: Id

    @Composable
    fun Content()

    /** A stable ID that uniquely identifies a route in the back stack. */
    @ConsistentCopyVisibility
    @Serializable
    data class Id private constructor(
        val value: String,
    ) {
        companion object {
            fun new(): Id {
                val seed = Random.nextInt()
                val buffer = Buffer()
                buffer.writeInt(seed)
                val bytes = buffer.readByteArray()
                val base64 = Base64.encode(bytes)
                return Id(base64)
            }

            @VisibleForTesting
            fun new(value: String): Id = Id(value)
        }
    }
}
