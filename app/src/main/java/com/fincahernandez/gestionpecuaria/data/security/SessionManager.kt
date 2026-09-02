package com.fincahernandez.gestionpecuaria.data.security

import android.content.Context

/** Conserva solamente el identificador de la cuenta cuando el usuario decide recordar sesión. */
class SessionManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "sesion_gestion_pecuaria",
        Context.MODE_PRIVATE
    )

    fun rememberedUserId(): String? = preferences.getString(KEY_USER_ID, null)

    fun remember(userId: String) {
        preferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_USER_ID).apply()
    }

    private companion object {
        const val KEY_USER_ID = "usuario_recordado_id"
    }
}
