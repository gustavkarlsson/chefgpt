package se.gustavkarlsson.chefgpt

import java.nio.file.Path

data class UserMessage(
    val text: String?,
    val image: Path?,
)
