package org.libraryofarts.core.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ArtworkEntity::class,
        JournalEntryEntity::class,
        CollectionEntity::class,
        CollectionArtworkEntity::class,
        TasteSignalEntity::class,
        InterestEntity::class,
        InstalledPackEntity::class,
        FlashcardEntity::class,
        ReadingProgressEntity::class,
        HiddenWorkEntity::class,
        SearchHistoryEntity::class,
        QuizResultEntity::class,
        SavedFilterEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao
    abstract fun journalDao(): JournalDao
    abstract fun collectionDao(): CollectionDao
    abstract fun tasteDao(): TasteDao
    abstract fun packDao(): PackDao
    abstract fun learningDao(): LearningDao
    abstract fun progressDao(): ProgressDao
    abstract fun searchDao(): SearchDao
}

object LibraryDatabaseFactory {
    fun create(context: Context, passphrase: String): LibraryDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))
        return Room.databaseBuilder(context, LibraryDatabase::class.java, "library_of_arts.db")
            .openHelperFactory(factory)
            .addCallback(Fts5Callback)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }
}

/**
 * Room does not model FTS5 virtual tables directly, so the encrypted database
 * owns an FTS5 index maintained by triggers. Search stays entirely on-device
 * and works with installed packs when the device is offline.
 */
/**
 * True only after the FTS5 table and its triggers have been created
 * successfully. Search falls back to LIKE when a vendor SQLite build omits
 * FTS5, so an unusual device never crashes or returns an empty library.
 */
object FtsSupport {
    @Volatile var available: Boolean = true
        internal set
}

private object Fts5Callback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        createFts(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        createFts(db)
    }

    private fun createFts(db: SupportSQLiteDatabase) {
        val created = runCatching { createFtsUnsafe(db) }.isSuccess
        FtsSupport.available = created
        if (!created) {
            Log.w("LibraryDatabase", "FTS5 is unavailable; search will use the LIKE fallback.")
        }
    }

    private fun createFtsUnsafe(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS artwork_fts USING fts5(
                artworkId UNINDEXED,
                title,
                artist,
                description,
                tags,
                museum,
                medium,
                genre,
                period,
                tokenize = 'unicode61 remove_diacritics 2'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS artwork_fts_insert AFTER INSERT ON artworks BEGIN
                INSERT INTO artwork_fts(artworkId, title, artist, description, tags, museum, medium, genre, period)
                VALUES (new.id, new.title, new.artistName, new.description, new.tagsCsv,
                        new.museumName, COALESCE(new.medium, ''), COALESCE(new.genre, ''), COALESCE(new.period, ''));
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS artwork_fts_update AFTER UPDATE ON artworks BEGIN
                DELETE FROM artwork_fts WHERE artworkId = old.id;
                INSERT INTO artwork_fts(artworkId, title, artist, description, tags, museum, medium, genre, period)
                VALUES (new.id, new.title, new.artistName, new.description, new.tagsCsv,
                        new.museumName, COALESCE(new.medium, ''), COALESCE(new.genre, ''), COALESCE(new.period, ''));
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS artwork_fts_delete AFTER DELETE ON artworks BEGIN
                DELETE FROM artwork_fts WHERE artworkId = old.id;
            END
            """.trimIndent(),
        )
    }
}
