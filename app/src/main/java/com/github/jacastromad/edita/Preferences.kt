package com.github.jacastromad.edita

import android.content.Context
import android.content.SharedPreferences

// Singleton object to manage app preferences
object Preferences {

    // Constants for preference keys and name
    private const val PREF_NAME = "edita_preferences"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_THEME = "theme"
    private const val KEY_NUM_LINES = "num_lines"
    private const val KEY_AUTO_INDENT = "auto_indent"
    private const val KEY_HL_SELECTION = "hl_selection"
    private const val KEY_SOFT_TABS = "soft_tabs"
    private const val KEY_TAB_STOP = "tab_stop"
    private const val KEY_SHOW_INV = "show_invisibles"
    private const val KEY_WORD_WRAP = "word_wrap"

    private lateinit var preferences: SharedPreferences
    private var isInitialized = false

    // Initialize the instance
    fun init(context: Context) {
        if (!isInitialized) {
            preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            isInitialized = true
        }
    }

    fun setTheme(value: String) {
        preferences.edit().putString(KEY_THEME, value).apply()
    }

    fun getTheme(defaultValue: String = "system"): String {
        return preferences.getString(KEY_THEME, defaultValue) ?: defaultValue
    }

    fun setFontSize(value: Float) {
        preferences.edit().putFloat(KEY_FONT_SIZE, value).apply()
    }

    fun getFontSize(defaultValue: Float = 14.0f): Float {
        return preferences.getFloat(KEY_FONT_SIZE, defaultValue)
    }

    fun setNumLines(value: Boolean) {
        preferences.edit().putBoolean(KEY_NUM_LINES, value).apply()
    }

    fun getNumLines(defaultValue: Boolean = true): Boolean {
        return preferences.getBoolean(KEY_NUM_LINES, defaultValue)
    }

    fun setAutoIndent(value: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_INDENT, value).apply()
    }

    fun getAutoIndent(defaultValue: Boolean = true): Boolean {
        return preferences.getBoolean(KEY_AUTO_INDENT, defaultValue)
    }

    fun setHlSelection(value: Boolean) {
        preferences.edit().putBoolean(KEY_HL_SELECTION, value).apply()
    }

    fun getHlSelection(defaultValue: Boolean = true): Boolean {
        return preferences.getBoolean(KEY_HL_SELECTION, defaultValue)
    }

    fun setSoftTabs(value: Boolean) {
        preferences.edit().putBoolean(KEY_SOFT_TABS, value).apply()
    }

    fun getSoftTabs(defaultValue: Boolean = true): Boolean {
        return preferences.getBoolean(KEY_SOFT_TABS, defaultValue)
    }

    fun setTabStop(value: Int) {
        preferences.edit().putInt(KEY_TAB_STOP, value).apply()
    }

    fun getTabStop(defaultValue: Int = 4): Int {
        return preferences.getInt(KEY_TAB_STOP, defaultValue)
    }

    fun setShowInvisibles(value: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_INV, value).apply()
    }

    fun getShowInvisibles(defaultValue: Boolean = false): Boolean {
        return preferences.getBoolean(KEY_SHOW_INV, defaultValue)
    }

    fun setWordWrap(value: Boolean) {
        preferences.edit().putBoolean(KEY_WORD_WRAP, value).apply()
    }

    fun getWordWrap(defaultValue: Boolean = true): Boolean {
        return preferences.getBoolean(KEY_WORD_WRAP, defaultValue)
    }

}
