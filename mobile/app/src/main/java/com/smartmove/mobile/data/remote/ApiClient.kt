package com.smartmove.mobile.data.remote

import com.smartmove.mobile.data.SessionStore
import com.smartmove.mobile.data.model.ApiErrorBody
import com.smartmove.mobile.data.model.FieldError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.TimeUnit

class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
    val fieldErrors: List<FieldError> = emptyList(),
) : Exception(message)

class ApiClient(
    private val sessionStore: SessionStore,
    private val json: Json,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    var onUnauthorized: (() -> Unit)? = null

    suspend fun get(path: String, params: Map<String, String?> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            execute("GET", path, params, body = null, headers = emptyMap())
        }

    suspend fun send(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) {
        execute(method, path, emptyMap(), body, headers)
    }

    suspend fun sendNoContent(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ) {
        withContext(Dispatchers.IO) {
            execute(method, path, emptyMap(), body, headers)
        }
    }

    suspend fun upload(
        path: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mediaType))
            .build()
        val request = Request.Builder()
            .url(buildUrl(path))
            .header("Accept", "application/json")
            .apply { sessionStore.accessToken()?.let { header("Authorization", "Bearer $it") } }
            .post(multipart)
            .build()
        executeRequest(request, path)
    }

    private fun execute(
        method: String,
        path: String,
        params: Map<String, String?>,
        body: String?,
        headers: Map<String, String>,
    ): String {
        val requestBody = when {
            body != null -> body.toRequestBody(JSON_MEDIA_TYPE)
            method == "GET" || method == "DELETE" -> null
            else -> ByteArray(0).toRequestBody(null)
        }
        val builder = Request.Builder()
            .url(buildUrl(path, params))
            .header("Accept", "application/json")
            .method(method, requestBody)

        headers.forEach { (name, value) -> builder.header(name, value) }
        sessionStore.accessToken()?.let { builder.header("Authorization", "Bearer $it") }

        return executeRequest(builder.build(), path)
    }

    private fun executeRequest(request: Request, path: String): String {
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 401 && !path.startsWith("auth/")) {
                    onUnauthorized?.invoke()
                }
                throw parseError(response.code, responseBody)
            }
            return responseBody
        }
    }

    private fun buildUrl(path: String, params: Map<String, String?> = emptyMap()): HttpUrl {
        val base = sessionStore.baseUrl().trimEnd('/')
        val url = "$base/${path.trimStart('/')}".toHttpUrl()
        return url.newBuilder().apply {
            params.forEach { (name, value) ->
                if (value != null) addQueryParameter(name, value)
            }
        }.build()
    }

    private fun parseError(status: Int, body: String): ApiException {
        val parsed = runCatching { json.decodeFromString<ApiErrorBody>(body) }.getOrNull()
        val message = parsed?.message?.takeIf { it.isNotBlank() }
            ?: defaultStatusMessage(status)
        return ApiException(
            status = status,
            code = parsed?.code?.takeIf { it.isNotBlank() } ?: "HTTP_$status",
            message = message,
            fieldErrors = parsed?.fieldErrors.orEmpty(),
        )
    }

    private fun defaultStatusMessage(status: Int): String = when (status) {
        401 -> "로그인이 만료되었습니다. 다시 로그인해 주세요."
        403 -> "이 요청을 처리할 권한이 없습니다."
        404 -> "요청한 정보를 찾을 수 없습니다."
        409 -> "현재 상태와 충돌하여 처리할 수 없습니다."
        429 -> "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."
        in 500..599 -> "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."
        else -> "요청을 처리하지 못했습니다. 입력 내용을 확인해 주세요."
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
