package se.gustavkarlsson.chefgpt.files

class FakeHtmlLoader : HtmlLoader {
    override suspend fun loadText(url: String): String = "Fake page text"
}
