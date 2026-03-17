package com.studentgig.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages JWT token storage using EncryptedSharedPreferences.
 * All credentials are AES-256 encrypted at rest.
 * Used by OkHttp interceptor to attach auth headers,
 * and by ViewModel to check login state (gatekeeper logic).
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "studentgig_secure_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular prefs if encryption is unavailable (e.g. very old devices)
        Log.w("TokenManager", "EncryptedSharedPreferences unavailable, using fallback", e)
        context.getSharedPreferences("studentgig_auth", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_SKILLS = "user_skills"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUser(id: Int, name: String, phone: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_PHONE, phone)
            .apply()
    }

    fun saveSkills(skillsJson: String) {
        prefs.edit().putString(KEY_USER_SKILLS, skillsJson).apply()
    }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserPhone(): String? = prefs.getString(KEY_USER_PHONE, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserSkills(): String? = prefs.getString(KEY_USER_SKILLS, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun logout() {
        prefs.edit().clear().apply()
    }
}
