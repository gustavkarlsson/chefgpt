package se.gustavkarlsson.chefgpt.agent.scaningredients

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile

interface ScanIngredientsAgent {
    suspend fun scan(
        userId: UserId,
        files: List<UploadedFile>,
    ): List<String>
}
