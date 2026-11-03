package com.smartmove.mobile.data.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductRecognitionResponseTest {

    @Test
    fun `decodes product and qr recognition response`() {
        val result = Json.decodeFromString<ProductRecognitionResponse>(
            """
            {
              "productName": "드럼 세탁기",
              "productType": "WASHING_MACHINE",
              "manufacturer": "제조사",
              "modelNumber": "WM-1234",
              "serialNumber": null,
              "capacity": "20kg",
              "confidence": 0.94,
              "keyFeatures": ["드럼형"],
              "qrCodeValue": "QR-123"
            }
            """.trimIndent(),
        )

        assertEquals("WASHING_MACHINE", result.productType)
        assertEquals("WM-1234", result.modelNumber)
        assertEquals("QR-123", result.qrCodeValue)
        assertEquals(listOf("드럼형"), result.keyFeatures)
        assertNull(result.serialNumber)
    }

    @Test
    fun `allows fields the vision service could not identify to be absent`() {
        val result = Json.decodeFromString<ProductRecognitionResponse>("""{"keyFeatures":[]}""")

        assertNull(result.productName)
        assertNull(result.productType)
        assertEquals(emptyList<String>(), result.keyFeatures)
    }
}
