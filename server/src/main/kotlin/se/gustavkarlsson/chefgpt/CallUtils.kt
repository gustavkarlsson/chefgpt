package se.gustavkarlsson.chefgpt

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toErrorIfNull
import com.github.michaelbull.result.toResultOr
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.basicAuthenticationCredentials
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.util.getOrFail
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.common.CLIENT_PLATFORM_HEADER
import se.gustavkarlsson.chefgpt.api.common.CLIENT_VERSION_HEADER
import se.gustavkarlsson.chefgpt.api.common.ChatId
import se.gustavkarlsson.chefgpt.api.errors.v1.ApiError
import se.gustavkarlsson.chefgpt.auth.Session
import se.gustavkarlsson.chefgpt.auth.SessionCookie
import se.gustavkarlsson.chefgpt.auth.toSession
import se.gustavkarlsson.chefgpt.chats.ChatRepository

fun ApplicationCall.getCredentials(): Result<UserPasswordCredential, ResponseData<ApiError?>> =
    runCatching { request.basicAuthenticationCredentials() }
        .mapError { throwable ->
            when (throwable) {
                is BadRequestException -> {
                    ResponseData(
                        status = HttpStatusCode.BadRequest,
                        body = ApiError("invalid-credentials", "Invalid credentials", userMessage = null),
                    )
                }

                else -> {
                    ResponseData(HttpStatusCode.InternalServerError)
                }
            }
        }.toErrorIfNull {
            ResponseData(
                status = HttpStatusCode.BadRequest,
                body = ApiError("missing-credentials", "Missing credentials", userMessage = null),
            )
        }

fun ApplicationCall.requireSession(): Session =
    checkNotNull(sessionOrNull()) {
        "User principal missing. Are we calling this in a non-authenticated endpoint?"
    }

fun ApplicationCall.sessionOrNull(): Session? = principal<SessionCookie>()?.toSession()

suspend fun ApplicationCall.getChatId(): Result<ChatId, ResponseData<ApiError>> {
    val chatRepository = get<ChatRepository>()
    val userId = requireSession().user.id
    val rawChatId = parameters.getOrFail("chatId")
    return ChatId
        .parseOrNull(rawChatId)
        .toResultOr {
            ResponseData(
                status = HttpStatusCode.BadRequest,
                body = ApiError("invalid-chat-id", "Invalid chat ID", userMessage = null),
            )
        }.flatMap { chatId ->
            chatRepository[userId, chatId].toResultOr {
                ResponseData(
                    status = HttpStatusCode.NotFound,
                    body = ApiError("chat-not-found", "Chat not found", userMessage = null),
                )
            }
        }.map { chat ->
            chat.id
        }
}

// Sent by every client on every request; useful for client-specific workarounds.
fun ApplicationCall.clientPlatform(): String? = request.headers[CLIENT_PLATFORM_HEADER]

fun ApplicationCall.clientVersion(): String? = request.headers[CLIENT_VERSION_HEADER]
