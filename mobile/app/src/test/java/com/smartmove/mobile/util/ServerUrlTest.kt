package com.smartmove.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerUrlTest {
    private val fallback = "http://10.0.2.2:8080/api/v1/"

    @Test
    fun addsSchemeAndApiPath() {
        assertEquals(
            "http://192.168.0.10:8080/api/v1/",
            ServerUrl.normalize("192.168.0.10:8080", fallback),
        )
    }

    @Test
    fun keepsExistingApiPath() {
        assertEquals(
            "https://example.com/api/v1/",
            ServerUrl.normalize("https://example.com/api/v1", fallback),
        )
    }

    @Test
    fun appendsApiPathToHostRoot() {
        assertEquals(
            "https://example.com/api/v1/",
            ServerUrl.normalize("https://example.com/", fallback),
        )
    }

    @Test
    fun usesFallbackWhenEmpty() {
        assertEquals(fallback, ServerUrl.normalize("", fallback))
    }
}
