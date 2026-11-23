package org.libraryofarts.core.database.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.libraryofarts.core.database.ArtworkDao
import org.libraryofarts.core.database.CollectionDao
import org.libraryofarts.core.database.JournalDao
import org.libraryofarts.core.database.LearningDao
import org.libraryofarts.core.database.LibraryDatabaseHolder
import org.libraryofarts.core.database.PackDao
import org.libraryofarts.core.database.ProgressDao
import org.libraryofarts.core.database.SearchDao
import org.libraryofarts.core.database.SecureDatabaseKey
import org.libraryofarts.core.database.TasteDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideSecureDatabaseKey(@ApplicationContext context: Context): SecureDatabaseKey =
        SecureDatabaseKey(context)

    @Provides
    @Singleton
    fun provideDatabaseHolder(
        @ApplicationContext context: Context,
        key: SecureDatabaseKey,
    ): LibraryDatabaseHolder = LibraryDatabaseHolder(context, key)

    @Provides fun artworkDao(holder: LibraryDatabaseHolder): ArtworkDao = holder.get().artworkDao()
    @Provides fun journalDao(holder: LibraryDatabaseHolder): JournalDao = holder.get().journalDao()
    @Provides fun collectionDao(holder: LibraryDatabaseHolder): CollectionDao = holder.get().collectionDao()
    @Provides fun tasteDao(holder: LibraryDatabaseHolder): TasteDao = holder.get().tasteDao()
    @Provides fun packDao(holder: LibraryDatabaseHolder): PackDao = holder.get().packDao()
    @Provides fun learningDao(holder: LibraryDatabaseHolder): LearningDao = holder.get().learningDao()
    @Provides fun progressDao(holder: LibraryDatabaseHolder): ProgressDao = holder.get().progressDao()
    @Provides fun searchDao(holder: LibraryDatabaseHolder): SearchDao = holder.get().searchDao()
}
