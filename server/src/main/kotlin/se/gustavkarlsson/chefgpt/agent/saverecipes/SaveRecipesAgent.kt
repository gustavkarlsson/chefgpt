package se.gustavkarlsson.chefgpt.agent.saverecipes

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.files.UploadedFile

interface SaveRecipesAgent {
    suspend fun scan(
        userId: UserId,
        images: List<UploadedFile>,
    ): List<String>
}
