package se.gustavkarlsson.chefgpt.files

class FakeAttachmentTextLoader : AttachmentTextLoader {
    override suspend fun loadText(url: String): String = "Fake attachment text"
}
