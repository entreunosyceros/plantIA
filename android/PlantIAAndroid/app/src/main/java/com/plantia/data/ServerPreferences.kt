package com.plantia.data

import android.content.Context
import com.plantia.Config

class ServerPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var baseUrl: String
        get() {
            val stored = prefs.getString(KEY_BASE_URL, null)?.trim().orEmpty()
            return normalizeBaseUrl(if (stored.isBlank()) Config.DEFAULT_BASE_URL else stored)
        }
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, normalizeBaseUrl(value)).apply()
        }

    companion object {
        private const val PREFS_NAME = "plantia_server"
        private const val KEY_BASE_URL = "base_url"

        fun normalizeBaseUrl(raw: String): String {
            var url = raw.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            if (!url.endsWith("/")) url += "/"
            return url
        }
    }
}
