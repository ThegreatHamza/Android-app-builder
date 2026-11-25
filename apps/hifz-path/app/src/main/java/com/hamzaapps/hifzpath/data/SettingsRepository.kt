package com.hamzaapps.hifzpath.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 0 = follow system, 1 = light, 2 = dark. */
typealias ThemeMode = Int

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = intPreferencesKey("theme_mode")
        val ACTIVE_STUDENT = longPreferencesKey("active_student")
        val PRO = booleanPreferencesKey("pro_unlocked")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { it[Keys.THEME] ?: 0 }
    val activeStudentId: Flow<Long?> = context.dataStore.data.map { it[Keys.ACTIVE_STUDENT] }
    val proUnlocked: Flow<Boolean> = context.dataStore.data.map { it[Keys.PRO] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.coerceIn(0, 2) }
    }

    suspend fun setActiveStudent(id: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_STUDENT] = id }
    }

    suspend fun setProUnlocked(value: Boolean) {
        context.dataStore.edit { it[Keys.PRO] = value }
    }
}
