package org.libraryofarts.core.datastore.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.libraryofarts.core.datastore.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun settingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)
}
