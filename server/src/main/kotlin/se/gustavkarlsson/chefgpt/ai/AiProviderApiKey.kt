package se.gustavkarlsson.chefgpt.ai

@JvmInline
value class AiProviderApiKey(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "API key may not be blank"
        }
    }

    override fun toString(): String = value
}
