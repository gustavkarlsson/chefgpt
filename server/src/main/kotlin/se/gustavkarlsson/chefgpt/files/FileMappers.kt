package se.gustavkarlsson.chefgpt.files

import se.gustavkarlsson.chefgpt.api.ApiUploadedFile

fun UploadedFile.toApi(): ApiUploadedFile = ApiUploadedFile(url, mimeType, fileName)

fun ApiUploadedFile.toDomain(): UploadedFile = UploadedFile(url, mimeType, fileName)
