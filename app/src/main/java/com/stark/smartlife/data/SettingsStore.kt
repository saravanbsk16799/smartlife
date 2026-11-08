package com.stark.smartlife.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT, STARK }

/** Non-sensitive UI preferences. Personal data lives in [SecureStore]. */
class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val languageKey = stringPreferencesKey("language")

    val theme: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[themeKey] ?: ThemeMode.STARK.name) }.getOrDefault(ThemeMode.STARK)
    }

    /** "" = follow system, otherwise a BCP-47 tag such as "ta". */
    val language: Flow<String> = context.dataStore.data.map { it[languageKey] ?: "" }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setLanguage(tag: String) {
        context.dataStore.edit { it[languageKey] = tag }
    }
}
