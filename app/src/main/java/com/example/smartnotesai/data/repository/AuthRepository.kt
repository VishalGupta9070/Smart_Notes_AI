package com.example.smartnotesai.data.repository

import android.content.Context
import android.content.SharedPreferences

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun login(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) return false

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("saved_email", email.trim())
            .apply()
        return true
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getSavedEmail(): String {
        return prefs.getString("saved_email", "")?.trim().orEmpty()
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", loggedIn).apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
