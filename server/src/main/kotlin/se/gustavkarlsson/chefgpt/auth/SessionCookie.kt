package se.gustavkarlsson.chefgpt.auth

import kotlinx.serialization.Serializable

// The serialized form of a session as stored in the session cookie. The domain Session is pure,
// so the cookie format lives here in the auth layer.

@Serializable
data class SessionCookie(
    val user: SessionCookieUser,
)

@Serializable
data class SessionCookieUser(
    val id: UserId,
    val name: String,
)

fun Session.toCookie(): SessionCookie = SessionCookie(SessionCookieUser(user.id, user.name))

fun SessionCookie.toSession(): Session = Session(User(user.id, user.name))
