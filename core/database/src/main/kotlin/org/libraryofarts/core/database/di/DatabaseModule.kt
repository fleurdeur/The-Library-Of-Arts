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
import org.libraryofarts.core.database.LibraryDatabase
import org.libraryofarts.core.database.LibraryDatabaseFactory
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
    fun provideDatabase(
        @ApplicationContext context: Context,
        key: SecureDatabaseKey,
    ): LibraryDatabase = LibraryDatabaseFactory.create(context, key.passphrase())

    @Provides fun artworkDao(db: LibraryDatabase): ArtworkDao = db.artworkDao()
    @Provides fun journalDao(db: LibraryDatabase): JournalDao = db.journalDao()
    @Provides fun collectionDao(db: LibraryDatabase): CollectionDao = db.collectionDao()
    @Provides fun tasteDao(db: LibraryDatabase): TasteDao = db.tasteDao()
    @Provides fun packDao(db: LibraryDatabase): PackDao = db.packDao()
    @Provides fun learningDao(db: LibraryDatabase): LearningDao = db.learningDao()
    @Provides fun progressDao(db: LibraryDatabase): ProgressDao = db.progressDao()
    @Provides fun searchDao(db: LibraryDatabase): SearchDao = db.searchDao()
}
