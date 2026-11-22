package org.libraryofarts.core.data.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.libraryofarts.core.common.Crypto
import org.libraryofarts.core.data.mapper.ArtworkJson
import org.libraryofarts.core.data.mapper.toArtwork
import org.libraryofarts.core.data.mapper.toEntity
import org.libraryofarts.core.database.ArtworkDao
import org.libraryofarts.core.database.CollectionArtworkEntity
import org.libraryofarts.core.database.CollectionDao
import org.libraryofarts.core.database.CollectionEntity
import org.libraryofarts.core.database.FlashcardEntity
import org.libraryofarts.core.database.JournalDao
import org.libraryofarts.core.database.JournalEntryEntity
import org.libraryofarts.core.database.LearningDao
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.Collection
import org.libraryofarts.core.model.JournalEntry
import org.libraryofarts.core.model.Mood

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val createdAt: Long,
    val artworks: List<Artwork> = emptyList(),
    val journal: List<JournalEntry> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val flashcards: List<FlashcardBackup> = emptyList(),
)

@Serializable
data class FlashcardBackup(
    val id: String,
    val front: String,
    val back: String,
    val artworkId: String?,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueAt: Long,
    val lapses: Int,
)

/**
 * Encrypted local backup, written through the Storage Access Framework so the
 * user chooses the destination. AES-256-GCM with a PBKDF2-derived key; without
 * the passphrase the file is unreadable. No cloud account is involved.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artworkDao: ArtworkDao,
    private val journalDao: JournalDao,
    private val collectionDao: CollectionDao,
    private val learningDao: LearningDao,
) {
    suspend fun exportTo(uri: Uri, passphrase: CharArray): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(passphrase.size >= 8) { "Use a passphrase of at least 8 characters." }
            val artworks = artworkDao.getAll().mapNotNull { it.toArtwork() }
            val journal = journalDao.all().map { it.toModel() }
            val collections = collectionDao.observeCollections().first().map { entity ->
                entity.toModel(
                    collectionDao.observeArtworkIds(entity.id).first().map { it.artworkId },
                )
            }
            val flashcards = learningDao.observeAll().first().map {
                FlashcardBackup(it.id, it.front, it.back, it.artworkId, it.easeFactor, it.intervalDays, it.repetitions, it.dueAt, it.lapses)
            }
            val payload = BackupPayload(
                createdAt = System.currentTimeMillis(),
                artworks = artworks,
                journal = journal,
                collections = collections,
                flashcards = flashcards,
            )
            val json = ArtworkJson.encodeToString(BackupPayload.serializer(), payload).toByteArray()
            val encrypted = Crypto.encryptBackup(json, passphrase)
            context.contentResolver.openOutputStream(uri)?.use { it.write(encrypted) }
                ?: error("The chosen file could not be opened.")
            payload.journal.size + payload.collections.size + payload.artworks.size
        }
    }

    suspend fun restoreFrom(uri: Uri, passphrase: CharArray): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("The chosen file could not be read.")
            val decrypted = Crypto.decryptBackup(bytes, passphrase)
            val payload = ArtworkJson.decodeFromString(BackupPayload.serializer(), decrypted.decodeToString())
            require(payload.schemaVersion <= 1) { "This backup comes from a newer version of the app." }
            artworkDao.upsertAll(payload.artworks.map { it.toEntity() })
            payload.journal.forEach { journalDao.upsert(it.toEntity()) }
            payload.collections.forEach { collection ->
                collectionDao.upsertCollection(collection.toEntity())
                collection.artworkIds.forEachIndexed { index, id ->
                    collectionDao.addArtwork(CollectionArtworkEntity(collection.id, id, System.currentTimeMillis(), index))
                }
            }
            payload.flashcards.forEach {
                learningDao.upsertCard(
                    FlashcardEntity(it.id, it.front, it.back, it.artworkId, it.easeFactor, it.intervalDays, it.repetitions, it.dueAt, it.lapses),
                )
            }
            payload.journal.size + payload.collections.size
        }
    }

    fun suggestFileName(): String = "library-of-arts-backup-${System.currentTimeMillis() / 86_400_000}.loa"

    fun cacheFile(name: String = suggestFileName()): File =
        File(context.cacheDir, name).apply { parentFile?.mkdirs() }

    private fun JournalEntryEntity.toModel(): JournalEntry = JournalEntry(
        id = id,
        artworkId = artworkId,
        artworkTitle = artworkTitle,
        artworkImageUrl = artworkImageUrl,
        text = text,
        mood = runCatching { Mood.valueOf(mood) }.getOrDefault(Mood.REFLECTIVE),
        tags = tagsCsv.split('|').filter { it.isNotBlank() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        audioNotePath = audioNotePath,
    )

    private fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
        id = id,
        artworkId = artworkId,
        artworkTitle = artworkTitle,
        artworkImageUrl = artworkImageUrl,
        text = text,
        mood = mood.name,
        tagsCsv = tags.joinToString("|"),
        createdAt = createdAt,
        updatedAt = updatedAt,
        audioNotePath = audioNotePath,
    )

    private fun CollectionEntity.toModel(ids: List<String>): Collection = Collection(
        id = id,
        name = name,
        description = description,
        artworkIds = ids,
        coverArtworkId = coverArtworkId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorites = isFavorites,
        tags = tagsCsv.split('|').filter { it.isNotBlank() },
    )

    private fun Collection.toEntity(): CollectionEntity = CollectionEntity(
        id = id,
        name = name,
        description = description,
        coverArtworkId = coverArtworkId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorites = isFavorites,
        tagsCsv = tags.joinToString("|"),
    )
}
