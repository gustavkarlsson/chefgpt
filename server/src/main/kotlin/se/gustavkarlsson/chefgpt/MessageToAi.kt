package se.gustavkarlsson.chefgpt

import java.nio.file.Path

data class MessageToAi(
    val text: String?,
    val image: Path?,
)
