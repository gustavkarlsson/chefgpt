package se.gustavkarlsson.chefgpt.debug

fun interface GetBaseUrl {
    suspend operator fun invoke(): String
}

class RealGetBaseUrl(
    private val settings: Settings,
) : GetBaseUrl {
    override suspend fun invoke(): String = settings.getBaseUrl()
}
