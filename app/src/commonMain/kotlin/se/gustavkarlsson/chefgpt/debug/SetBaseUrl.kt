package se.gustavkarlsson.chefgpt.debug

fun interface SetBaseUrl {
    suspend operator fun invoke(value: String)
}

class RealSetBaseUrl(
    private val settings: Settings,
) : SetBaseUrl {
    override suspend fun invoke(value: String) = settings.setBaseUrl(value)
}
