package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("api-job")
data class ApiJob<T>(
    val id: JobId,
    val state: ApiJobState,
    val createdAt: Instant,
    val finishedAt: Instant?,
    val result: T?,
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
