package se.gustavkarlsson.chefgpt.util

import io.ktor.server.routing.Route
import io.ktor.server.sse.ServerSSESessionWithSerialization
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.koin.ktor.ext.get
import kotlin.time.Duration.Companion.seconds

fun Route.sse(
    path: String,
    handler: suspend ServerSSESessionWithSerialization.() -> Unit,
) {
    sse(
        path,
        serialize = { typeInfo, value ->
            val json = get<Json>()
            val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
            json.encodeToString(serializer, value)
        },
    ) {
        heartbeat {
            period = 15.seconds
        }
        handler()
    }
}
