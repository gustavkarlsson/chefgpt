package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.toMap
import org.slf4j.event.Level

fun Application.installCallLogging() {
    val config = environment.config
    install(CallLogging) {
        val configLevelString = config.propertyOrNull("logging.calls.level")?.getString()?.uppercase()
        val configLevel = Level.entries.find { it.name == configLevelString }
        level = configLevel ?: Level.INFO

        val requestHeaders = config.propertyOrNull("logging.calls.request.headers")?.getString().toBoolean()
        if (requestHeaders) {
            mdc("request.headers") { call ->
                call.request.headers
                    .toMap()
                    .toString()
            }
        }

        val responseHeaders = config.propertyOrNull("logging.calls.response.headers")?.getString().toBoolean()
        if (responseHeaders) {
            mdc("response.headers") { call ->
                call.response.headers
                    .allValues()
                    .toMap()
                    .toString()
            }
        }

        format { call ->
            buildString {
                append("${call.response.status()}: ")
                append(call.request.httpMethod)
                append(" ")
                append(call.request.path())
                append(" in ${call.processingTimeMillis()}ms")
            }
        }
    }
}
