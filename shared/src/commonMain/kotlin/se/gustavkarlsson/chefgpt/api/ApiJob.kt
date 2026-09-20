package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant

@Serializable
@SerialName("api-job")
data class ApiJob(
    val id: JobId,
    val state: ApiJobState,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val result: JsonElement?,
    val error: ApiError?,
)

@Serializable
enum class ApiJobState {
    @SerialName("working")
    Working,

    @SerialName("success")
    Success,

    @SerialName("failure")
    Failure,
}
