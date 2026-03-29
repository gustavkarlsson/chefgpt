package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.toMap
import org.slf4j.event.Level

fun Application.installCallLogging(config: ApplicationConfig) {
    install(CallLogging) {
        val callConfig = config.config("logging.calls")
        val configLevelString = callConfig.propertyOrNull("level")?.getString()?.uppercase()
        val configLevel = Level.entries.find { it.name == configLevelString }
        level = configLevel ?: Level.INFO

        val requestHeaders = callConfig.propertyOrNull("request.headers")?.getString().toBoolean()
        if (requestHeaders) {
            mdc("request.headers") { call ->
                call.request.headers
                    .toMap()
                    .toString()
            }
        }

        val responseHeaders = callConfig.propertyOrNull("response.headers")?.getString().toBoolean()
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
