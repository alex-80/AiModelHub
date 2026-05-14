package com.ai_model_hub.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteModelDao {

    @Query("SELECT * FROM remote_models ORDER BY name")
    fun modelsFlow(): Flow<List<RemoteModelEntity>>

    @Query("SELECT * FROM remote_models WHERE modelId = :modelId LIMIT 1")
    suspend fun getByModelId(modelId: String): RemoteModelEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: RemoteModelEntity)

    @Update
    suspend fun update(entity: RemoteModelEntity)

    @Transaction
    suspend fun upsertOrUpdateModels(entities: List<RemoteModelEntity>) {
        entities.forEach { new ->
            val existing = getByModelId(new.modelId)
            if (existing == null) {
                insert(new)
            } else {
                update(new.copy())
            }
        }
    }

    @Transaction
    suspend fun insertWhenNotExists(entities: List<RemoteModelEntity>) {
        entities.forEach { new ->
            val existing = getByModelId(new.modelId)
            if (existing == null) {
                insert(new)
            }
        }
    }
}
