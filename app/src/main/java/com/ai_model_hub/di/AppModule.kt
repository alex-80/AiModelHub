package com.ai_model_hub.di

import android.content.Context
import androidx.room.Room
import com.ai_model_hub.data.DefaultDownloadRepository
import com.ai_model_hub.data.DownloadRepository
import com.ai_model_hub.data.db.AppDatabase
import com.ai_model_hub.data.db.RemoteModelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideDownloadRepository(
        @ApplicationContext context: Context,
    ): DownloadRepository = DefaultDownloadRepository(context)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ai_model_hub.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun provideRemoteModelDao(db: AppDatabase): RemoteModelDao = db.remoteModelDao()
}
