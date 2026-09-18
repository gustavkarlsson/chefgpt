package se.gustavkarlsson.chefgpt.navigation

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.screens.start.StartScreen
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

// TODO Add logging

class Navigator(
    initialScreen: Screen = StartScreen(),
) {
    val backStack: StateFlow<List<Screen>>
        field = MutableStateFlow(listOf(initialScreen))

    private val pending = mutableMapOf<ScreenRequestId, PendingRequest>()

    fun push(screen: Screen) {
        backStack.update { routes -> routes + screen }
        pruneRequests()
    }

    fun replaceTop(screen: Screen) {
        backStack.update { routes -> routes.dropLast(1) + screen }
        pruneRequests()
    }

    fun pop() {
        backStack.update { routes -> routes.dropLast(1) }
        pruneRequests()
    }

    suspend fun <R : Any> requestResult(
        screen: Screen.ResultProvider<R>,
        serializer: KSerializer<R>,
    ): R? =
        suspendCancellableCoroutine { continuation: CancellableContinuation<R?> ->
            val requesterId = backStack.value.last().id
            val requestId = ScreenRequestId.new()
            pending[requestId] =
                PendingRequest(
                    request = ScreenRequest(requestId, requesterId, screen.id, resultJson = null),
                    continuation = continuation,
                )
            continuation.invokeOnCancellation { pending.remove(requestId) }
            push(screen)
        }

    fun <R : Any> completeResult(
        screen: Screen.ResultProvider<R>,
        result: R,
        serializer: KSerializer<R>,
    ) {
        val entry = pending.values.firstOrNull { it.request.targetId == screen.id } ?: return
        if (!entry.continuation.isActive) return
        val resultJson = chefGptJson(strict = true).encodeToString(serializer, result)
        pending[entry.request.id] = PendingRequest(entry.request.copy(resultJson = resultJson), entry.continuation)
        @Suppress("UNCHECKED_CAST")
        (entry.continuation as Continuation<R?>).resume(result)
    }

    @VisibleForTesting
    internal fun pendingRequests(): Map<ScreenRequestId, ScreenRequest> =
        pending.mapValues { (_, entry) -> entry.request }

    private fun pruneRequests() {
        val stackIds = backStack.value.mapTo(mutableSetOf()) { it.id }
        val stale =
            pending.values.filter { entry ->
                entry.request.resultJson != null ||
                    entry.request.targetId !in stackIds ||
                    entry.request.requesterId !in stackIds
            }
        for (entry in stale) {
            pending.remove(entry.request.id)
            if (entry.request.resultJson == null && entry.continuation.isActive) {
                @Suppress("UNCHECKED_CAST")
                (entry.continuation as Continuation<Any?>).resume(null)
            }
        }
    }

    private class PendingRequest(
        val request: ScreenRequest,
        val continuation: CancellableContinuation<*>,
    )
}
