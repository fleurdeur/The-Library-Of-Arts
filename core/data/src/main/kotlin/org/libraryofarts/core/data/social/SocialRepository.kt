package org.libraryofarts.core.data.social

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

data class ReelComment(
    val id: String,
    val artworkId: String,
    val author: String,
    val text: String,
    val createdAt: Long,
)

/**
 * The reels' social layer. Every note and every name lives on this device —
 * there is no account, no analytics and nothing leaves the phone except the
 * anonymous like tally the library publishes in its own repository.
 */
@Singleton
class SocialRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val prefs by lazy { context.getSharedPreferences("library_social", Context.MODE_PRIVATE) }
    private val countMutex = Mutex()

    @Serializable
    private data class CommentRecord(
        val id: String,
        val artworkId: String,
        val author: String,
        val text: String,
        val createdAt: Long,
    )

    @Serializable
    private data class RemoteCounts(val likes: Map<String, Int> = emptyMap())

    fun displayName(): String = prefs.getString(KEY_NAME, null)
        ?: randomName().also { prefs.edit().putString(KEY_NAME, it).apply() }

    fun randomName(): String {
        val first = listOf(
            "Quiet", "Amber", "Vermilion", "Umber", "Sage", "Marble", "Ink", "Chalk",
            "Ochre", "Slate", "Sepia", "Indigo", "Crimson", "Willow", "Bronze", "Linen",
        )
        val second = listOf(
            "Eye", "Frame", "Brush", "Plate", "Etching", "Gaze", "Studio", "Gallery",
            "Canvas", "Sketch", "Chiaroscuro", "Perspective", "Vignette", "Study",
        )
        return "${first.random()}-${second.random()}${(100..999).random()}"
    }

    /** The name can change at most once every 30 days, verified by network time. */
    suspend fun changeName(newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.length < 3 || trimmed.length > 32) return false
        val serverTime = fetchServerTime() ?: (prefs.getLong(KEY_SERVER_ANCHOR, 0L) + (System.currentTimeMillis() - prefs.getLong(KEY_LOCAL_ANCHOR, 0L))).takeIf { prefs.getLong(KEY_SERVER_ANCHOR, 0L) > 0 }
        if (serverTime == null) return false
        prefs.edit()
            .putLong(KEY_SERVER_ANCHOR, serverTime)
            .putLong(KEY_LOCAL_ANCHOR, System.currentTimeMillis())
            .apply()
        val last = prefs.getLong(KEY_NAME_CHANGED_AT, 0L)
        if (last > 0 && serverTime - last < TimeUnit.DAYS.toMillis(30)) return false
        prefs.edit()
            .putString(KEY_NAME, trimmed)
            .putLong(KEY_NAME_CHANGED_AT, serverTime)
            .apply()
        return true
    }

    /** Milliseconds left before the name may change again. */
    fun nameChangeCooldownLeft(): Long {
        val serverAnchor = prefs.getLong(KEY_SERVER_ANCHOR, 0L)
        val localAnchor = prefs.getLong(KEY_LOCAL_ANCHOR, 0L)
        if (serverAnchor == 0L) return 0L
        val serverNow = serverAnchor + (System.currentTimeMillis() - localAnchor).coerceAtLeast(0)
        val last = prefs.getLong(KEY_NAME_CHANGED_AT, 0L)
        val left = TimeUnit.DAYS.toMillis(30) - (serverNow - last)
        return left.coerceAtLeast(0)
    }

    suspend fun fetchServerTime(): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                response.header("Date")?.let { httpDate ->
                    java.time.ZonedDateTime.parse(
                        httpDate,
                        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME,
                    ).toInstant().toEpochMilli()
                }
            }
        }.getOrNull()
    }

    fun isLiked(artworkId: String): Boolean = prefs.getStringSet(KEY_LIKES, emptySet()).orEmpty().contains(artworkId)

    fun toggleLike(artworkId: String): Boolean {
        val current = prefs.getStringSet(KEY_LIKES, emptySet()).orEmpty().toMutableSet()
        val added = if (current.contains(artworkId)) {
            current.remove(artworkId)
            false
        } else {
            current.add(artworkId)
            true
        }
        prefs.edit().putStringSet(KEY_LIKES, current).apply()
        return added
    }

    fun likedCount(): Int = prefs.getStringSet(KEY_LIKES, emptySet()).orEmpty().size

    /** Global tally published by the library, cached for six hours. */
    suspend fun remoteLikeCounts(): Map<String, Int> = countMutex.withLock {
        withContext(Dispatchers.IO) {
            val cachedAt = prefs.getLong(KEY_COUNTS_AT, 0L)
            val cached = prefs.getString(KEY_COUNTS, null)
            if (cached != null && System.currentTimeMillis() - cachedAt < TimeUnit.HOURS.toMillis(6)) {
                return@withContext runCatching { json.decodeFromString<RemoteCounts>(cached).likes }.getOrDefault(emptyMap())
            }
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/fleurdeur/The-Library-Of-Arts/main/docs/social.json")
                .header("Accept", "application/json")
                .build()
            val body = runCatching {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull()
            val counts = body?.let {
                runCatching { json.decodeFromString<RemoteCounts>(it).likes }.getOrDefault(emptyMap())
            }
            if (!counts.isNullOrEmpty()) {
                prefs.edit()
                    .putString(KEY_COUNTS, json.encodeToString(RemoteCounts(counts)))
                    .putLong(KEY_COUNTS_AT, System.currentTimeMillis())
                    .apply()
            }
            counts.orEmpty()
        }
    }

    fun comments(artworkId: String): List<ReelComment> = runCatching {
        json.decodeFromString<List<CommentRecord>>(prefs.getString(KEY_COMMENTS, "[]").orEmpty())
            .filter { it.artworkId == artworkId }
            .sortedByDescending { it.createdAt }
            .map { ReelComment(it.id, it.artworkId, it.author, it.text, it.createdAt) }
    }.getOrDefault(emptyList())

    fun addComment(artworkId: String, text: String): ReelComment? {
        val clean = text.trim()
        if (clean.isEmpty() || clean.length > 280) return null
        val record = CommentRecord(
            id = "c-${System.currentTimeMillis()}",
            artworkId = artworkId,
            author = displayName(),
            text = clean,
            createdAt = System.currentTimeMillis(),
        )
        val all = runCatching {
            json.decodeFromString<List<CommentRecord>>(prefs.getString(KEY_COMMENTS, "[]").orEmpty())
        }.getOrDefault(emptyList())
        prefs.edit().putString(KEY_COMMENTS, json.encodeToString(all + record)).apply()
        return ReelComment(record.id, record.artworkId, record.author, record.text, record.createdAt)
    }

    fun removeComment(commentId: String) {
        val all = runCatching {
            json.decodeFromString<List<CommentRecord>>(prefs.getString(KEY_COMMENTS, "[]").orEmpty())
        }.getOrDefault(emptyList())
        prefs.edit().putString(KEY_COMMENTS, json.encodeToString(all.filterNot { it.id == commentId })).apply()
    }

    /** Images saved from reels, stored inside the app's own data. */
    fun galleryDir(): java.io.File = java.io.File(context.filesDir, "gallery").apply { mkdirs() }

    fun galleryImages(): List<java.io.File> =
        galleryDir().listFiles()?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "png", "jpeg") }
            ?.sortedByDescending { it.lastModified() }.orEmpty()

    private companion object {
        const val KEY_NAME = "display_name"
        const val KEY_NAME_CHANGED_AT = "name_changed_at"
        const val KEY_SERVER_ANCHOR = "server_time_anchor"
        const val KEY_LOCAL_ANCHOR = "local_time_anchor"
        const val KEY_LIKES = "likes"
        const val KEY_COMMENTS = "comments"
        const val KEY_COUNTS = "like_counts"
        const val KEY_COUNTS_AT = "like_counts_at"
    }
}
