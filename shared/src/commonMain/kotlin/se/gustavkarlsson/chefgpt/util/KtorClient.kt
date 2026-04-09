package se.gustavkarlsson.chefgpt.util

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.sse.deserialize
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

suspend inline fun <reified T : Any> HttpClient.sseTyped(
    json: Json,
    eventType: String,
    noinline request: HttpRequestBuilder.() -> Unit,
    crossinline handler: suspend (HttpClientCall, Flow<T>) -> Unit,
) {
    sse(
        deserialize = { typeInfo, text ->
            val serializer = json.serializersModule.serializer(typeInfo.kotlinType!!)
            json.decodeFromString(serializer, text)
        },
        request = request,
    ) {
        val incomingTyped =
            incoming
                .filter {
                    it.event == eventType
                }.map {
                    checkNotNull(it.data) {
                        "SSE event data was null: ${it.event}"
                    }
                }.map {
                    checkNotNull(deserialize<T>(it)) {
                        "SSE event data could not be deserialized to ${T::class.simpleName}: $it"
                    }
                }
        handler(call, incomingTyped)
    }
}
