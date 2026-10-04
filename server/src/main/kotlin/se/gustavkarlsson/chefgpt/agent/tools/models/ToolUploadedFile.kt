package se.gustavkarlsson.chefgpt.agent.tools.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import se.gustavkarlsson.chefgpt.files.UploadedFile

@Serializable
@LLMDescription("A file the user has uploaded.")
data class ToolUploadedFile(
    @property:LLMDescription("The url of the file")
    val url: String,
    @property:LLMDescription("The mime type of this file, e.g. image/png")
    val mimeType: String,
    @property:LLMDescription("The name the file had on the user's device, if it had one.")
    val fileName: String?,
)

fun UploadedFile.toTool(): ToolUploadedFile = ToolUploadedFile(url, mimeType, fileName)

fun ToolUploadedFile.toDomain(): UploadedFile = UploadedFile(url, mimeType, fileName)
