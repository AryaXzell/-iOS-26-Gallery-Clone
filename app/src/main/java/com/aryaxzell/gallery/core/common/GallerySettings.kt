package com.aryaxzell.gallery.core.common

import android.content.Context
import android.os.Build
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_settings")

data class GallerySettings(
    val reduceTransparency: Boolean = false,
    val reduceMotion: Boolean = false,
    val liquidGlassEnabled: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
)

val LocalGallerySettings = compositionLocalOf { GallerySettings() }

class GallerySettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val REDUCE_TRANSPARENCY = booleanPreferencesKey("reduce_transparency")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
    }

    val settingsFlow: Flow<GallerySettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val defaultLiquidGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            GallerySettings(
                reduceTransparency = preferences[PreferencesKeys.REDUCE_TRANSPARENCY] ?: false,
                reduceMotion = preferences[PreferencesKeys.REDUCE_MOTION] ?: false,
                liquidGlassEnabled = preferences[PreferencesKeys.LIQUID_GLASS_ENABLED] ?: defaultLiquidGlass
            )
        }

    suspend fun updateSettings(reduceTransparency: Boolean, reduceMotion: Boolean, liquidGlassEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REDUCE_TRANSPARENCY] = reduceTransparency
            preferences[PreferencesKeys.REDUCE_MOTION] = reduceMotion
            preferences[PreferencesKeys.LIQUID_GLASS_ENABLED] = liquidGlassEnabled
        }
    }
}
