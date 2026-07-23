package com.learnsy.admin.data

import android.content.Context

enum class CardBlurLevel(val stored: String) { OFF("off"), FIFTY("50"), EIGHTY_FIVE("85") }

// Tương đương các localStorage key trong SettingsPanel: learnsy_admin_name,
// learnsy_school, learnsy_card_blur, và dark mode toggle.
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("learnsy_admin", Context.MODE_PRIVATE)

    var adminName: String
        get() = prefs.getString(KEY_NAME, "Admin") ?: "Admin"
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var school: String
        get() = prefs.getString(KEY_SCHOOL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SCHOOL, value).apply()

    var cardBlur: CardBlurLevel
        get() = when (prefs.getString(KEY_BLUR, "off")) {
            "50" -> CardBlurLevel.FIFTY
            "85" -> CardBlurLevel.EIGHTY_FIVE
            else -> CardBlurLevel.OFF
        }
        set(value) = prefs.edit().putString(KEY_BLUR, value.stored).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    // Tương đương localStorage 'bb_admin_theme'
    var themePreset: String
        get() = prefs.getString(KEY_THEME, "default") ?: "default"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    companion object {
        private const val KEY_NAME = "learnsy_admin_name"
        private const val KEY_SCHOOL = "learnsy_school"
        private const val KEY_BLUR = "learnsy_card_blur"
        private const val KEY_DARK = "learnsy_dark_mode"
        private const val KEY_THEME = "bb_admin_theme"
    }
}
