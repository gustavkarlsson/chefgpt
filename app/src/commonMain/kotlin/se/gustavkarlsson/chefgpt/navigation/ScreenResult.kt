package se.gustavkarlsson.chefgpt.navigation

import androidx.annotation.VisibleForTesting
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.io.encoding.Base64
import kotlin.random.Random

/**
 * A stable ID that uniquely identifies a single result request.
 *
 * Mirrors [Screen.Id]; a request only ever lives in the navigator's in-memory
 * pending map, so a random value is enough and is never read back from an older
 * instance.
 */
@ConsistentCopyVisibility
@Serializable
data class ScreenRequestId private constructor(
    val value: String,
) {
    companion object {
        fun new(): ScreenRequestId {
            val seed = Random.nextInt()
            val buffer = Buffer()
            buffer.writeInt(seed)
            val bytes = buffer.readByteArray()
            val base64 = Base64.encode(bytes)
            return ScreenRequestId(base64)
        }

        @VisibleForTesting
        fun new(value: String): ScreenRequestId = ScreenRequestId(value)
    }
}

/**
 * An in-flight result request, keyed by [id].
 *
 * [resultJson] holds the serialized result once the target delivers it; a non-null
 * value marks the request consumed. The request stays in the navigator until the
 * requester consumes it or both sides leave the back stack.
 */
@Serializable
data class ScreenRequest(
    val id: ScreenRequestId,
    val requesterId: Screen.Id,
    val targetId: Screen.Id,
    val resultJson: String?,
)

suspend inline fun <reified R : Any> Navigator.requestResult(screen: Screen.ResultProvider<R>): R? =
    requestResult(screen, serializer())

inline fun <reified R : Any> Navigator.completeResult(
    screen: Screen.ResultProvider<R>,
    result: R,
) {
    completeResult(screen, result, serializer())
}
