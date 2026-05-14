package com.ai_model_hub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai_model_hub.sdk.BackendPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_manager_prefs")

@Singleton
class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val DOWNLOADED_MODELS_KEY = stringSetPreferencesKey("downloaded_models")
    private val ENABLED_MODELS_KEY = stringSetPreferencesKey("enabled_models")
    private val BACKEND_PREFERENCE_KEY = stringPreferencesKey("preferred_backend")
    private val SPECULATIVE_DECODING_KEY = booleanPreferencesKey("speculative_decoding_enabled")

    val downloadedModels: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOADED_MODELS_KEY] ?: emptySet()
    }

    val enabledModels: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[ENABLED_MODELS_KEY] ?: emptySet()
    }

    val backendPreference: Flow<BackendPreference> = context.dataStore.data.map { prefs ->
        when (prefs[BACKEND_PREFERENCE_KEY]) {
            BackendPreference.GPU.name -> BackendPreference.GPU
            else -> BackendPreference.CPU
        }
    }

    val speculativeDecoding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SPECULATIVE_DECODING_KEY] ?: false
    }

    suspend fun markModelDownloaded(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[DOWNLOADED_MODELS_KEY] ?: emptySet()
            prefs[DOWNLOADED_MODELS_KEY] = current + id
        }
    }

    suspend fun markModelDeleted(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[DOWNLOADED_MODELS_KEY] ?: emptySet()
            prefs[DOWNLOADED_MODELS_KEY] = current - id
        }
        // Also disable the model when deleted
        setModelEnabled(id, false)
    }

    suspend fun setModelEnabled(id: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[ENABLED_MODELS_KEY] ?: emptySet()
            prefs[ENABLED_MODELS_KEY] = if (enabled) current + id else current - id
        }
    }

    suspend fun setBackendPreference(pref: BackendPreference) {
        context.dataStore.edit { prefs ->
            prefs[BACKEND_PREFERENCE_KEY] = pref.name
        }
    }

    suspend fun setSpeculativeDecoding(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SPECULATIVE_DECODING_KEY] = enabled
        }
    }

    fun isModelDownloaded(id: String, downloadedModels: Set<String>): Boolean {
        return id in downloadedModels
    }
}
