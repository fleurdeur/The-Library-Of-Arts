package org.libraryofarts.core.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

enum class UpdateArchitecture(val label: String, val note: String) {
    UNIVERSAL("Universal", "Works on every phone and tablet"),
    ARM64("ARM64", "Most modern phones and tablets"),
    ARMV7("ARMv7", "Older 32-bit devices"),
}

@Serializable
data class UpdateAsset(
    val architecture: UpdateArchitecture,
    val name: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String? = null,
) {
    val sizeLabel: String
        get() = when {
            sizeBytes >= 1L shl 20 -> "%.1f MB".format(sizeBytes / 1024.0 / 1024.0)
            sizeBytes >= 1024 -> "%.0f KB".format(sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
}

@Serializable
data class AppUpdate(
    val tag: String,
    val versionName: String,
    val releaseName: String,
    val notes: String,
    val publishedAt: String = "",
    val assets: List<UpdateAsset> = emptyList(),
) {
    val versionCode: Int get() = parseVersion(versionName)
}

data class UpdateCheck(
    val currentVersion: String,
    val latest: AppUpdate? = null,
    val available: Boolean = false,
    val error: String? = null,
    val fromCache: Boolean = false,
)

data class UpdateDownloadProgress(
    val bytes: Long = 0,
    val total: Long = 0,
    val state: State = State.IDLE,
    val error: String? = null,
) {
    enum class State { IDLE, DOWNLOADING, VERIFYING, READY, FAILED }
    val fraction: Float get() = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f
}

sealed interface UpdateInstallResult {
    data class Started(val file: File) : UpdateInstallResult
    data object PermissionRequired : UpdateInstallResult
    data class Failed(val message: String) : UpdateInstallResult
}

/**
 * Talks only to the public GitHub release API. It never sends personal data,
 * uses the shared OkHttp cache and keeps a six-hour cached result so repeated
 * screen visits cannot exhaust GitHub's unauthenticated rate limit.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val prefs by lazy { context.getSharedPreferences("library_updates", Context.MODE_PRIVATE) }

    suspend fun check(currentVersion: String, force: Boolean = false): UpdateCheck =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cached = readCache()
            if (!force && cached != null && now - prefs.getLong(KEY_CHECKED_AT, 0L) < CACHE_MILLIS) {
                return@withContext cached.toCheck(currentVersion, fromCache = true)
            }

            val request = Request.Builder()
                .url("https://api.github.com/repos/fleurdeur/The-Library-Of-Arts/releases?per_page=20")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            val response = runCatching { client.newCall(request).execute() }.getOrNull()
            response.use { result ->
                if (result == null) {
                    return@withContext cached?.toCheck(currentVersion, fromCache = true)
                        ?: UpdateCheck(currentVersion, error = "GitHub could not be reached. Check your connection and try again.")
                }
                if (!result.isSuccessful) {
                    val friendly = when (result.code) {
                        403, 429 -> "GitHub is limiting update checks for a little while. The app will try again later."
                        404 -> "The release feed could not be found."
                        else -> "GitHub returned ${result.code}. Try again in a moment."
                    }
                    return@withContext cached?.toCheck(currentVersion, fromCache = true)?.copy(error = friendly)
                        ?: UpdateCheck(currentVersion, error = friendly)
                }
                val body = result.body?.string().orEmpty()
                val releases = runCatching { json.decodeFromString<List<GitHubRelease>>(body) }.getOrNull()
                    ?: return@withContext cached?.toCheck(currentVersion, fromCache = true)
                        ?: UpdateCheck(currentVersion, error = "The release feed was not readable.")
                val latest = releases
                    .asSequence()
                    .filter { !it.draft && !it.prerelease }
                    .filter { STABLE_TAG.matches(it.tagName) }
                    .mapNotNull { it.toUpdate() }
                    .maxByOrNull { it.versionCode }
                    ?: return@withContext UpdateCheck(currentVersion, error = "No published app release was found.")

                prefs.edit()
                    .putLong(KEY_CHECKED_AT, now)
                    .putString(KEY_CACHE, json.encodeToString(latest))
                    .apply()
                latest.toCheck(currentVersion)
            }
        }

    suspend fun download(
        asset: UpdateAsset,
        onProgress: (UpdateDownloadProgress) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = updateDir()
            val destination = File(dir, asset.name)
            val existing = destination.takeIf { it.exists() && it.length() == asset.sizeBytes }
            if (existing != null && asset.sha256.isNullOrBlank()) {
                onProgress(UpdateDownloadProgress(existing.length(), existing.length(), UpdateDownloadProgress.State.READY))
                return@runCatching existing
            }

            val part = File(dir, "${asset.name}.part")
            onProgress(UpdateDownloadProgress(0, asset.sizeBytes, UpdateDownloadProgress.State.DOWNLOADING))
            val request = Request.Builder()
                .url(asset.url)
                .header("Accept", "application/octet-stream")
                .build()
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) throw IllegalStateException("The APK download returned ${res.code}.")
                val body = res.body ?: throw IllegalStateException("The APK download was empty.")
                val total = asset.sizeBytes.takeIf { it > 0 } ?: body.contentLength()
                var written = 0L
                var lastTick = 0L
                FileOutputStream(part, false).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(128 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            written += read
                            val now = System.currentTimeMillis()
                            if (now - lastTick > 150) {
                                lastTick = now
                                onProgress(UpdateDownloadProgress(written, total, UpdateDownloadProgress.State.DOWNLOADING))
                            }
                        }
                    }
                }
                onProgress(UpdateDownloadProgress(written, total, UpdateDownloadProgress.State.VERIFYING))
                val expected = asset.sha256?.removePrefix("sha256:")?.lowercase()
                if (!expected.isNullOrBlank()) {
                    val actual = sha256(part)
                    if (!actual.equals(expected, ignoreCase = true)) {
                        part.delete()
                        throw IllegalStateException("The APK checksum did not match. It was deleted.")
                    }
                }
                if (!part.renameTo(destination)) {
                    part.delete()
                    throw IllegalStateException("The downloaded APK could not be saved.")
                }
                onProgress(UpdateDownloadProgress(destination.length(), destination.length(), UpdateDownloadProgress.State.READY))
                destination
            }
        }.onFailure {
            onProgress(UpdateDownloadProgress(state = UpdateDownloadProgress.State.FAILED, error = it.message ?: "The update could not be downloaded."))
        }
    }

    fun install(file: File, versionName: String): UpdateInstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return UpdateInstallResult.PermissionRequired
        }
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            prefs.edit().putString(KEY_PENDING_VERSION, versionName).apply()
            UpdateInstallResult.Started(file)
        }.getOrElse { UpdateInstallResult.Failed(it.message ?: "The installer could not be opened.") }
    }

    fun permissionIntent(): Intent = Intent(
        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installedVersionCode(): Int = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt() else @Suppress("DEPRECATION") it.versionCode
        }
    }.getOrDefault(0)

    fun installedVersionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    /** Deletes an update APK only after the installed app has caught up. */
    fun cleanUpIfInstalled() {
        val pending = prefs.getString(KEY_PENDING_VERSION, null) ?: return
        if (compareVersions(installedVersionName(), pending) >= 0) {
            updateDir().deleteRecursively()
            prefs.edit().remove(KEY_PENDING_VERSION).remove(KEY_READY_FILE).remove(KEY_READY_VERSION).apply()
        }
    }

    fun downloadedFile(asset: UpdateAsset): File? = File(updateDir(), asset.name).takeIf { it.exists() }

    fun readyDownload(): File? {
        val assetName = prefs.getString(KEY_READY_FILE, null) ?: return null
        return File(updateDir(), assetName).takeIf { it.exists() }
    }

    fun readyVersion(): String? = prefs.getString(KEY_READY_VERSION, null)

    fun rememberReady(asset: UpdateAsset, versionName: String) {
        prefs.edit()
            .putString(KEY_READY_FILE, asset.name)
            .putString(KEY_READY_VERSION, versionName)
            .apply()
    }

    fun shouldNotify(update: AppUpdate): Boolean {
        if (update.versionCode <= parseVersion(installedVersionName())) return false
        return prefs.getString(KEY_NOTIFIED, null) != update.tag
    }

    fun markNotified(update: AppUpdate) {
        prefs.edit().putString(KEY_NOTIFIED, update.tag).apply()
    }

    fun clearAllDownloads() {
        updateDir().deleteRecursively()
        prefs.edit().remove(KEY_READY_FILE).remove(KEY_READY_VERSION).remove(KEY_PENDING_VERSION).apply()
    }

    private fun updateDir(): File = File(context.cacheDir, "updates").apply { mkdirs() }

    private fun readCache(): AppUpdate? = prefs.getString(KEY_CACHE, null)?.let {
        runCatching { json.decodeFromString<AppUpdate>(it) }.getOrNull()
    }

    private fun AppUpdate.toCheck(currentVersion: String, fromCache: Boolean = false): UpdateCheck {
        val installed = installedVersionName().takeIf { it.isNotBlank() } ?: currentVersion
        return UpdateCheck(
            currentVersion = currentVersion,
            latest = this,
            available = compareVersions(versionName, installed) > 0,
            fromCache = fromCache,
        )
    }

    private fun GitHubRelease.toUpdate(): AppUpdate? {
        val version = tagName.removePrefix("v")
        if (version.isBlank()) return null
        val mapped = assets.mapNotNull { asset ->
            if (asset.name.contains("wear", ignoreCase = true)) return@mapNotNull null
            val architecture = when {
                asset.name.contains("universal", true) -> UpdateArchitecture.UNIVERSAL
                asset.name.contains("arm64", true) || asset.name.contains("aarch64", true) -> UpdateArchitecture.ARM64
                asset.name.contains("armeabi-v7a", true) || asset.name.contains("armv7", true) -> UpdateArchitecture.ARMV7
                else -> null
            } ?: return@mapNotNull null
            UpdateAsset(
                architecture = architecture,
                name = asset.name,
                url = asset.browserDownloadUrl,
                sizeBytes = asset.size,
                sha256 = asset.digest?.removePrefix("sha256:"),
            )
        }.distinctBy { it.architecture }.sortedBy { it.architecture.ordinal }
        return AppUpdate(
            tag = tagName,
            versionName = version,
            releaseName = name.ifBlank { "The Great Library of Arts $version" },
            notes = body.orEmpty(),
            publishedAt = publishedAt.orEmpty(),
            assets = mapped,
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class GitHubRelease(
        @kotlinx.serialization.SerialName("tag_name") val tagName: String = "",
        val name: String = "",
        val body: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        @kotlinx.serialization.SerialName("published_at") val publishedAt: String? = null,
        val assets: List<GitHubAsset> = emptyList(),
    )

    private data class GitHubAsset(
        val name: String = "",
        @kotlinx.serialization.SerialName("browser_download_url") val browserDownloadUrl: String = "",
        val size: Long = 0,
        val digest: String? = null,
    )

    private companion object {
        const val CACHE_MILLIS = 6L * 60 * 60 * 1000
        const val KEY_CHECKED_AT = "last_checked_at"
        const val KEY_CACHE = "last_release"
        const val KEY_PENDING_VERSION = "pending_version"
        const val KEY_READY_FILE = "ready_file"
        const val KEY_READY_VERSION = "ready_version"
        const val KEY_NOTIFIED = "last_notified_tag"
        val STABLE_TAG = Regex("""^v\d+\.\d+\.\d+$""")
    }
}

fun parseVersion(value: String): Int {
    val parts = value.trim().removePrefix("v").split('.', '-', '+').mapNotNull { it.toIntOrNull() }
    val major = parts.getOrNull(0) ?: 0
    val minor = parts.getOrNull(1) ?: 0
    val patch = parts.getOrNull(2) ?: 0
    return major * 10_000 + minor * 100 + patch
}

fun compareVersions(a: String, b: String): Int = parseVersion(a).compareTo(parseVersion(b))
