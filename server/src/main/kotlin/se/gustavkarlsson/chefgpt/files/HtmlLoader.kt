package se.gustavkarlsson.chefgpt.files

interface HtmlLoader {
    /** Returns null if the page's html could not be read. */
    suspend fun loadText(url: String): String?
}
