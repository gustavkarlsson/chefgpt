package se.gustavkarlsson.chefgpt.api.recipes.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.api.files.v1.ApiUploadedFile

@Serializable
@SerialName("api-scan-recipe")
data class ApiScanRecipe(
    val files: List<ApiUploadedFile>,
)
