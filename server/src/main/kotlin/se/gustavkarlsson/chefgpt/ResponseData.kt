package se.gustavkarlsson.chefgpt

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.merge
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

data class ResponseData<out T>(
    val status: HttpStatusCode,
    val body: T,
) {
    companion object {
        operator fun invoke(status: HttpStatusCode) = ResponseData(status, null)
    }
}

suspend fun ApplicationCall.respond(responseData: ResponseData<*>) {
    if (responseData.body != null) {
        respond(responseData.status, responseData.body)
    } else {
        respond(responseData.status)
    }
}

suspend fun Result<ResponseData<*>, ResponseData<*>>.respond(call: ApplicationCall) {
    val responseData = merge()
    if (responseData.body != null) {
        call.respond(responseData.status, responseData.body)
    } else {
        call.respond(responseData.status)
    }
}
