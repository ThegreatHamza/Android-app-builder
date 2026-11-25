package com.hamzaapps.sitecalc.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hamzaapps.sitecalc.domain.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = intPreferencesKey("theme_mode")
        val CURRENCY = stringPreferencesKey("currency")
        val BAG_KG = doublePreferencesKey("bag_kg")
        val PRO = booleanPreferencesKey("pro_unlocked")
        val LAUNCHES = intPreferencesKey("launch_count")
        fun price(m: Material) = doublePreferencesKey("price_${m.name}")
    }

    /** 0 = follow system, 1 = light, 2 = dark. */
    val themeMode: Flow<Int> = context.dataStore.data.map { it[Keys.THEME] ?: 0 }
    val currency: Flow<String> = context.dataStore.data.map { it[Keys.CURRENCY] ?: "" }
    val bagKg: Flow<Double> = context.dataStore.data.map { it[Keys.BAG_KG] ?: 50.0 }
    val proUnlocked: Flow<Boolean> = context.dataStore.data.map { it[Keys.PRO] ?: false }
    val launchCount: Flow<Int> = context.dataStore.data.map { it[Keys.LAUNCHES] ?: 0 }
    val prices: Flow<Map<Material, Double>> = context.dataStore.data.map { prefs ->
        Material.entries.mapNotNull { m -> prefs[Keys.price(m)]?.let { m to it } }.toMap()
    }

    suspend fun setThemeMode(mode: Int) = context.dataStore.edit { it[Keys.THEME] = mode.coerceIn(0, 2) }
    suspend fun setCurrency(value: String) = context.dataStore.edit { it[Keys.CURRENCY] = value.trim().take(6) }
    suspend fun setBagKg(value: Double) = context.dataStore.edit { it[Keys.BAG_KG] = value }
    suspend fun setProUnlocked(value: Boolean) = context.dataStore.edit { it[Keys.PRO] = value }
    suspend fun incrementLaunches() = context.dataStore.edit { it[Keys.LAUNCHES] = (it[Keys.LAUNCHES] ?: 0) + 1 }

    suspend fun setPrice(material: Material, value: Double?) = context.dataStore.edit {
        if (value == null || value <= 0) it.remove(Keys.price(material)) else it[Keys.price(material)] = value
    }
}
