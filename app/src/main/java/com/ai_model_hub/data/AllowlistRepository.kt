package com.ai_model_hub.data

import android.content.Context
import android.util.Log
import com.ai_model_hub.data.db.RemoteModelDao
import com.ai_model_hub.data.db.RemoteModelEntity
import com.ai_model_hub.data.db.toEntity
import com.ai_model_hub.data.remote.RemoteAllowlist
import com.ai_model_hub.data.remote.RemoteVersions
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AllowlistRepository"
private const val ALLOWLIST_BASE_URL =
    "https://raw.githubusercontent.com/alex-80/AiModelHub/refs/heads/main/model_allowlists"
private const val VERSIONS_FILE = "versions.json"

@Singleton
class AllowlistRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: RemoteModelDao,
) {
    private val gson = Gson()
    private val _updatableModelsFlow = MutableStateFlow<List<RemoteModelEntity>>(emptyList())

    val modelsFlow: Flow<List<RemoteModelEntity>> = dao.modelsFlow()
    val updatableModelsFlow: Flow<List<RemoteModelEntity>> = _updatableModelsFlow.asSharedFlow()

    suspend fun maybeInitFromAssets() = withContext(Dispatchers.IO) {
        try {
            val versionCode = getVersionCode()
            val versionsJson = context.assets.open(VERSIONS_FILE).bufferedReader().readText()
            val remoteVersions = gson.fromJson(versionsJson, RemoteVersions::class.java)
            val allowlistFile = remoteVersions.resolveAllowlist(versionCode)
            if (allowlistFile == null) {
                Log.w(TAG, "No allowlist mapping for versionCode=$versionCode in assets.")
                return@withContext
            }
            val allowlistJson = context.assets.open(allowlistFile).bufferedReader().readText()
            val entities = parseEntities(allowlistJson)
            dao.insertWhenNotExists(entities)
            Log.d(TAG, "Initialized ${entities.size} models from assets ($allowlistFile).")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init from assets: ${e.message}")
        }
    }

    suspend fun loadFromRemote() = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val versionCode = getVersionCode()
            val versionsJson = fetchJson("$ALLOWLIST_BASE_URL/$VERSIONS_FILE")
            val remoteVersions = gson.fromJson(versionsJson, RemoteVersions::class.java)
            val allowlistFile = remoteVersions.resolveAllowlist(versionCode)
            if (allowlistFile == null) {
                Log.w(TAG, "No allowlist mapping for versionCode=$versionCode in remote versions.")
                return@withContext
            }
            val allowlistJson = fetchJson("$ALLOWLIST_BASE_URL/$allowlistFile")
            val entities = parseEntities(allowlistJson)
            _updatableModelsFlow.emit(entities)
            Log.d(TAG, "Synced ${entities.size} models from remote ($allowlistFile).")
        } catch (e: Exception) {
            Log.w(TAG, "Remote sync failed (non-critical): ${e.message}")
        }
    }

    suspend fun updateInstalled(model: RemoteModelEntity) {
        dao.update(model)
    }

    suspend fun getByModelId(modelId: String): RemoteModelEntity? =
        withContext(Dispatchers.IO) { dao.getByModelId(modelId) }

    private fun getVersionCode(): Int {
        @Suppress("DEPRECATION")
        val versionCode = context.packageManager
            .getPackageInfo(context.packageName, 0).versionCode
        return versionCode
    }

    private fun parseEntities(json: String): List<RemoteModelEntity> {
        val remote = gson.fromJson(json, RemoteAllowlist::class.java)
        return remote.models.map { it.toEntity() }
    }

    private fun fetchJson(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return try {
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }
}
