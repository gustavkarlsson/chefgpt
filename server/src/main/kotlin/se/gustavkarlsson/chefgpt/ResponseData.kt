package se.gustavkarlsson.chefgpt

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.merge
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

data class ResponseData<out T : Any>(
    val status: HttpStatusCode,
    val body: T? = null,
)

suspend fun Result<ResponseData<*>, ResponseData<*>>.respond(call: ApplicationCall) {
    val responseData = merge()
    if (responseData.body != null) {
        call.respond(responseData.status, responseData.body)
    } else {
        call.respond(responseData.status)
    }
}
