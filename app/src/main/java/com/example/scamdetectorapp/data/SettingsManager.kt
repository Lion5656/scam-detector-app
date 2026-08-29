package com.example.scamdetectorapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// 定義 DataStore 擴充屬性
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scam_detector_settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val KEY_PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        private val KEY_CONTACTS_ENABLED = booleanPreferencesKey("contacts_enabled")
        private val KEY_SHARE_AUTO_INPUT = booleanPreferencesKey("share_auto_input")
        private val KEY_PROTECTED_APPS = stringSetPreferencesKey("protected_apps")
        private val KEY_CUSTOM_WHITELIST = stringSetPreferencesKey("custom_whitelist")

        private val DEFAULT_PROTECTED_APPS = setOf(
            "com.esunbank",
            "com.esunbank.oneyou",
            "jp.naver.line.android",
            "com.linecorp.line.android",
            "com.facebook.orca",
            "com.google.android.youtube",
            "com.linepaytw.upay",
            "tw.gov.post.mpost",
            "com.linebank.tw"
        )
    }

    // 讀取「主動防護」開關
    val isProtectionEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[KEY_PROTECTION_ENABLED] ?: true
        }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROTECTION_ENABLED] = enabled
        }
    }

    // 讀取「聯絡人資料」開關
    val isContactsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[KEY_CONTACTS_ENABLED] ?: false
        }

    suspend fun setContactsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONTACTS_ENABLED] = enabled
        }
    }

    // 讀取「外部分享一鍵帶入」開關
    val isShareAutoInputEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[KEY_SHARE_AUTO_INPUT] ?: true
        }

    suspend fun setShareAutoInputEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHARE_AUTO_INPUT] = enabled
        }
    }

    // 讀取「受保護 App」清單
    val protectedApps: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[KEY_PROTECTED_APPS] ?: DEFAULT_PROTECTED_APPS
        }

    suspend fun updateProtectedApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROTECTED_APPS] = apps
        }
    }

    // 讀取「自定義白名單」清單
    val customWhitelist: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[KEY_CUSTOM_WHITELIST] ?: emptySet()
        }

    suspend fun updateCustomWhitelist(whitelist: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_WHITELIST] = whitelist
        }
    }
}
