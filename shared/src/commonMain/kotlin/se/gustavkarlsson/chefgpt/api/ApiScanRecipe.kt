package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("api-scan-recipe")
data class ApiScanRecipe(
    val attachments: List<ApiAttachment>,
)
