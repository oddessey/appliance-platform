package com.smartmove.mobile.util

import com.smartmove.mobile.data.remote.ApiException
import java.io.IOException

fun Throwable.userMessage(defaultMessage: String = "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."): String =
    when (this) {
        is ApiException -> {
            val fieldMessages = fieldErrors.mapNotNull { it.reason }.filter { it.isNotBlank() }
            when {
                fieldMessages.isNotEmpty() -> fieldMessages.joinToString(" ")
                message.isNotBlank() -> message
                else -> defaultMessage
            }
        }

        is IOException -> "서버에 연결할 수 없습니다. 네트워크와 서버 주소를 확인해 주세요."
        else -> message?.takeIf { it.isNotBlank() } ?: defaultMessage
    }
