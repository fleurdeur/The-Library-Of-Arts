package org.libraryofarts.core.database

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the single Room instance and validates it before any screen receives a
 * DAO. If the encrypted file cannot be opened — a changed Keystore key, a
 * corrupt WAL, or a database left behind by an older key scheme — the holder
 * closes it, removes the unusable file and rebuilds a clean library. That is
 * the difference between a recoverable library and an app that can never get
 * past "The library could not be opened just now."
 */
@Singleton
class LibraryDatabaseHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val key: SecureDatabaseKey,
) {
    @Volatile
    private var database: LibraryDatabase? = null

    @Synchronized
    fun get(): LibraryDatabase {
        database?.let { return it }

        val first = LibraryDatabaseFactory.create(context, key.passphrase())
        val opened = runCatching {
            first.openHelper.writableDatabase
        }
        if (opened.isSuccess) {
            database = first
            return first
        }

        Log.e("LibraryDatabase", "The library could not be opened; rebuilding a clean copy.", opened.exceptionOrNull())
        runCatching { first.close() }
        runCatching { key.clearAllOnReset() }
        context.deleteDatabase(SecureDatabaseKey.DB_NAME)
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            runCatching { File(context.getDatabasePath(SecureDatabaseKey.DB_NAME).path + suffix).delete() }
        }

        val second = LibraryDatabaseFactory.create(context, key.passphrase())
        second.openHelper.writableDatabase
        database = second
        return second
    }
}
