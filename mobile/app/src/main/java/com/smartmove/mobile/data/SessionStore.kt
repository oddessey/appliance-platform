package com.smartmove.mobile.data

import android.content.Context
import com.smartmove.mobile.data.model.AuthResponse
import com.smartmove.mobile.data.model.UserRole
import com.smartmove.mobile.data.model.UserSummary
import com.smartmove.mobile.util.ServerUrl
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SessionSnapshot(
    val accessToken: String?,
    val user: UserSummary?,
    val baseUrl: String,
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank() && user != null
}

class SessionStore(
    context: Context,
    private val json: Json,
    defaultBaseUrl: String,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val fallbackBaseUrl = ServerUrl.normalize(defaultBaseUrl, defaultBaseUrl)

    fun load(): SessionSnapshot {
        val baseUrl = ServerUrl.normalize(
            preferences.getString(KEY_BASE_URL, null).orEmpty(),
            fallbackBaseUrl,
        )
        return SessionSnapshot(
            accessToken = accessToken(),
            user = loadUser(),
            baseUrl = baseUrl,
        )
    }

    fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)
        ?.takeIf { it.isNotBlank() }

    fun baseUrl(): String = ServerUrl.normalize(
        preferences.getString(KEY_BASE_URL, null).orEmpty(),
        fallbackBaseUrl,
    )

    fun saveAuth(response: AuthResponse, baseUrl: String) {
        preferences.edit()
            .putString(KEY_BASE_URL, ServerUrl.normalize(baseUrl, fallbackBaseUrl))
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_USER, json.encodeToString(response.user))
            .apply()
    }

    fun saveBaseUrl(baseUrl: String) {
        preferences.edit()
            .putString(KEY_BASE_URL, ServerUrl.normalize(baseUrl, fallbackBaseUrl))
            .apply()
    }

    fun saveUser(user: UserSummary) {
        preferences.edit()
            .putString(KEY_USER, json.encodeToString(user))
            .apply()
    }

    fun saveSelectedRole(role: UserRole) {
        preferences.edit()
            .putString(KEY_SELECTED_ROLE, role.name)
            .apply()
    }

    fun loadSelectedRole(): UserRole? = preferences.getString(KEY_SELECTED_ROLE, null)
        ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }

    fun clearAuth() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USER)
            .remove(KEY_SELECTED_ROLE)
            .apply()
    }

    private fun loadUser(): UserSummary? {
        val raw = preferences.getString(KEY_USER, null) ?: return null
        return runCatching { json.decodeFromString<UserSummary>(raw) }.getOrNull()
    }

    private companion object {
        const val PREFERENCES_NAME = "smartmove.session"
        const val KEY_BASE_URL = "base_url"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_USER = "user"
        const val KEY_SELECTED_ROLE = "selected_role"
    }
}
