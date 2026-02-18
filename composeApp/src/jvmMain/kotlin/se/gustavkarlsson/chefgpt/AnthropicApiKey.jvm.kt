package se.gustavkarlsson.chefgpt

actual fun getAnthropicApiKey(): String = System.getenv("ANTHROPIC_API_KEY")