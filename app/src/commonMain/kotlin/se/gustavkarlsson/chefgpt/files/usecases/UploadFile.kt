package se.gustavkarlsson.chefgpt.files.usecases

import com.github.michaelbull.result.Result
import io.ktor.http.ContentType
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface UploadFile {
    suspend operator fun invoke(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): Result<ApiUploadedFile, ClientError>
}

class HttpUploadFile(
    private val client: ChefGptClient,
) : UploadFile {
    override suspend fun invoke(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): Result<ApiUploadedFile, ClientError> = client.uploadFile(sessionId, data, contentType)
}
