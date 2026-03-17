package org.messanger.project

import com.russhwolf.settings.Settings

class SessionStorage(
    private val settings: Settings
) {
    companion object {
        private const val KEY_TOKEN = "session_token"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_LOGIN = "session_login"
        private const val KEY_DISPLAY_NAME = "session_display_name"
    }

    fun saveSession(session: UserSession) {
        settings.putString(KEY_TOKEN, session.token)
        settings.putString(KEY_USER_ID, session.userId)
        settings.putString(KEY_LOGIN, session.login)
        settings.putString(KEY_DISPLAY_NAME, session.displayName)
    }

    fun loadSession(): UserSession? {
        val token = settings.getStringOrNull(KEY_TOKEN) ?: return null
        val userId = settings.getStringOrNull(KEY_USER_ID) ?: return null
        val login = settings.getStringOrNull(KEY_LOGIN) ?: return null
        val displayName = settings.getStringOrNull(KEY_DISPLAY_NAME) ?: return null

        return UserSession(
            token = token,
            userId = userId,
            login = login,
            displayName = displayName
        )
    }

    fun clearSession() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_LOGIN)
        settings.remove(KEY_DISPLAY_NAME)
    }
}