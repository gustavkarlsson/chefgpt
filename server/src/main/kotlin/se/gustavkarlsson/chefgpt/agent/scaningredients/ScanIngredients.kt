package se.gustavkarlsson.chefgpt.agent.scaningredients

import ai.koog.prompt.message.AttachmentSource

fun interface ScanIngredients {
    suspend operator fun invoke(
        images: List<AttachmentSource.Image>,
        existingIngredients: List<String>,
    ): String
}
