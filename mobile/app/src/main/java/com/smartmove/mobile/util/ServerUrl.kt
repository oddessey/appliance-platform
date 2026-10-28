package com.smartmove.mobile.util

object ServerUrl {
    fun normalize(input: String, fallback: String): String {
        var value = input.trim()
        if (value.isEmpty()) {
            value = fallback.trim()
        }
        if (!value.startsWith("http://", ignoreCase = true) &&
            !value.startsWith("https://", ignoreCase = true)
        ) {
            value = "http://$value"
        }
        value = value.trimEnd('/')
        val schemeEnd = value.indexOf("://")
        val pathStart = value.indexOf('/', schemeEnd + 3)
        if (pathStart == -1 || value.substring(pathStart).isBlank()) {
            value = "$value/api/v1"
        }
        return "$value/"
    }
}
