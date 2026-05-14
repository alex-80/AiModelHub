package com.ai_model_hub.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RemoteModelEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remoteModelDao(): RemoteModelDao
}
