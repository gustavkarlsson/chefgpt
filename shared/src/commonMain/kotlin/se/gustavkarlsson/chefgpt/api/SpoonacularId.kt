package se.gustavkarlsson.chefgpt.api

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class SpoonacularId(
    val value: Long,
)
