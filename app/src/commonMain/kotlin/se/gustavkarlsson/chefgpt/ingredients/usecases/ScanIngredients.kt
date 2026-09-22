package se.gustavkarlsson.chefgpt.ingredients.usecases

import com.github.michaelbull.result.Result
import io.ktor.http.ContentType
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.api.ApiJob
import se.gustavkarlsson.chefgpt.sessions.SessionId

fun interface ScanIngredients {
    suspend operator fun invoke(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): Result<ApiJob<List<String>>, ClientError>
}

class HttpScanIngredients(
    private val client: ChefGptClient,
) : ScanIngredients {
    override suspend fun invoke(
        sessionId: SessionId,
        data: Path,
        contentType: ContentType,
    ): Result<ApiJob<List<String>>, ClientError> = client.scanIngredients(sessionId, data, contentType)
}
