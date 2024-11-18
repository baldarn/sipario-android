package it.baldarn.sipario

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsHelper {
    private const val PREFS_NAME = "siparioPrefs"
    private const val KEY_JWT_TOKEN = "jwtToken"
    private const val NOTIFICATION_TOKEN = "notificationToken"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun saveJwtToken(context: Context, token: String?) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_JWT_TOKEN, token)
        editor.apply()
    }
    fun getJwtToken(context: Context): String? {
        return getPrefs(context).getString(KEY_JWT_TOKEN, null)
    }
    fun getNotificationToken(context: Context): String? {
        return getPrefs(context).getString(NOTIFICATION_TOKEN, null)
    }
    fun saveNotificationToken(context: Context, token: String) {
        val editor = getPrefs(context).edit()
        editor.putString(NOTIFICATION_TOKEN, token)
        editor.apply()
    }
}
