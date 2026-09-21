package se.gustavkarlsson.chefgpt.agent.scaningredients

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile

class FakeScanIngredientsAgent : ScanIngredientsAgent {
    override suspend fun scan(
        userId: UserId,
        files: List<UploadedFile>,
    ): List<String> = listOf("tomato", "basil")
}
