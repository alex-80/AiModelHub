package com.ai_model_hub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai_model_hub.sdk.BackendPreference
import com.google.gson.Gson
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
    private val gson = Gson()

    private val DOWNLOADED_MODELS_KEY = stringSetPreferencesKey("downloaded_models")
    private val ENABLED_MODELS_KEY = stringSetPreferencesKey("enabled_models")
    private val BACKEND_PREFERENCE_KEY = stringPreferencesKey("preferred_backend")

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

    suspend fun markModelDownloaded(modelName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[DOWNLOADED_MODELS_KEY] ?: emptySet()
            prefs[DOWNLOADED_MODELS_KEY] = current + modelName
        }
    }

    suspend fun markModelDeleted(modelName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[DOWNLOADED_MODELS_KEY] ?: emptySet()
            prefs[DOWNLOADED_MODELS_KEY] = current - modelName
        }
        // Also disable the model when deleted
        setModelEnabled(modelName, false)
    }

    suspend fun setModelEnabled(modelName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[ENABLED_MODELS_KEY] ?: emptySet()
            prefs[ENABLED_MODELS_KEY] = if (enabled) current + modelName else current - modelName
        }
    }

    suspend fun setBackendPreference(pref: BackendPreference) {
        context.dataStore.edit { prefs ->
            prefs[BACKEND_PREFERENCE_KEY] = pref.name
        }
    }

    fun isModelDownloaded(modelName: String, downloadedModels: Set<String>): Boolean {
        return modelName in downloadedModels
    }
}
