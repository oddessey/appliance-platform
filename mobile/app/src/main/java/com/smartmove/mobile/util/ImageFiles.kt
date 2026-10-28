package com.smartmove.mobile.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class PickedImage(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

fun readPickedImage(context: Context, uri: Uri): PickedImage? {
    val resolver = context.contentResolver
    val metadata = resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val name = cursor.getString(0)
            val size = if (cursor.isNull(1)) null else cursor.getLong(1)
            name to size
        } else {
            null
        }
    }
    val fileName = metadata?.first ?: "evidence.jpg"
    val rawMimeType = resolver.getType(uri)?.lowercase()
    val mimeType = when (rawMimeType) {
        "image/jpg", "image/pjpeg" -> "image/jpeg"
        null -> when (fileName.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        else -> rawMimeType
    }
    val size = metadata?.second
    if (size != null && size > MAX_IMAGE_BYTES) {
        throw IllegalArgumentException("이미지는 10MB 이하만 업로드할 수 있습니다.")
    }
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedImage(fileName = fileName, mimeType = mimeType, bytes = bytes)
}

private const val MAX_IMAGE_BYTES = 10L * 1024 * 1024
