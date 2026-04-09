package se.gustavkarlsson.chefgpt.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import java.util.concurrent.ConcurrentHashMap

class RepoSyncer<T : Any> {
    private val listenersByKey = ConcurrentHashMap<T, MutableSet<Listener>>()

    fun notifications(key: T): Flow<Unit> =
        channelFlow {
            val listener = Listener()
            try {
                listenersByKey.compute(key) { _, existingListeners ->
                    val listeners = existingListeners ?: ConcurrentHashMap.newKeySet()
                    listeners.add(listener)
                    listeners
                }
                listener.notifications.collect(::send)
            } finally {
                listenersByKey.computeIfPresent(key) { _, listeners ->
                    listeners.remove(listener)
                    listeners.ifEmpty { null } // Drop the map entry if now empty
                }
            }
        }

    fun notifyChange(key: T) {
        val listeners = listenersByKey[key] ?: return
        for (listener in listeners) {
            listener.notifyChange()
        }
    }

    private class Listener {
        private val mutableNotifications =
            MutableSharedFlow<Unit>(
                replay = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            ).apply { tryEmit(Unit) }

        val notifications: SharedFlow<Unit> = mutableNotifications.asSharedFlow()

        fun notifyChange() {
            mutableNotifications.tryEmit(Unit)
        }
    }
}
