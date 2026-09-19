package se.gustavkarlsson.chefgpt.agent

import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import se.gustavkarlsson.chefgpt.api.ApiUploadedFile
import se.gustavkarlsson.chefgpt.files.FileKind
import se.gustavkarlsson.chefgpt.files.UploadedFile
import se.gustavkarlsson.chefgpt.files.fileKindOrNull

fun UploadedFile.toImageAttachmentOrNull(): AttachmentSource.Image? {
    if (fileKindOrNull(mimeType) != FileKind.Image) return null
    return AttachmentSource.Image(
        content = AttachmentContent.URL(url),
        format = mimeType.substringAfter('/', mimeType),
        mimeType = mimeType,
        fileName = fileName,
    )
}

fun ApiUploadedFile.toDomain() =
    UploadedFile(
        url = url,
        mimeType = mimeType,
        fileName = fileName,
    )
