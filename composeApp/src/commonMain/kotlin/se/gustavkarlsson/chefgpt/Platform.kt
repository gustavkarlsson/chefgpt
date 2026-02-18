package se.gustavkarlsson.chefgpt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform