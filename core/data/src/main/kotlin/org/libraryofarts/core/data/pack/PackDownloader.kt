package org.libraryofarts.core.data.pack

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libraryofarts.core.common.Crypto
import org.libraryofarts.core.model.PackDownloadProgress
import org.libraryofarts.core.model.PackDownloadState
import org.libraryofarts.core.model.PackManifest

@Singleton
class PackDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttp: OkHttpClient,
) {
    suspend fun download(
        manifest: PackManifest,
        onProgress: (PackDownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "pack-downloads").apply { mkdirs() }
        val part = File(dir, "${manifest.id}-${manifest.version}.zip.part")
        val destination = File(dir, "${manifest.id}-${manifest.version}.zip")
        if (destination.exists() && manifest.sha256.isNotBlank()) {
            val ok = runCatching {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                destination.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
                    .equals(manifest.sha256, true)
            }.getOrDefault(false)
            if (ok) {
                onProgress(progress(manifest, destination.length(), manifest.sizeBytes, PackDownloadState.COMPLETE))
                return@withContext destination
            }
        }

        var existing = if (part.exists()) part.length() else 0L
        val request = Request.Builder()
            .url(manifest.downloadUrl)
            .apply { if (existing > 0) header("Range", "bytes=$existing-") }
            .build()
        val response = okHttp.newCall(request).execute()
        response.use { res ->
            if (!res.isSuccessful) {
                if (res.code == 416) {
                    part.delete()
                    existing = 0
                    throw IllegalStateException("The download cannot be resumed. Start again.")
                }
                throw IllegalStateException("The pack server returned ${res.code}. Try again later.")
            }
            val append = res.code == 206
            if (!append) {
                part.delete()
                existing = 0
            }
            val body = res.body ?: throw IllegalStateException("The pack download was empty.")
            val total = manifest.sizeBytes.takeIf { it > 0 } ?: (existing + body.contentLength())
            var downloaded = existing
            FileOutputStream(part, append).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(128 * 1024)
                    var lastTick = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastTick > 300) {
                            lastTick = now
                            onProgress(progress(manifest, downloaded, total, PackDownloadState.DOWNLOADING))
                        }
                        if (downloaded > total + 1024 * 1024) {
                            throw IllegalStateException("The server sent more data than expected. Download cancelled.")
                        }
                    }
                }
            }
            output(part)
        }

        onProgress(progress(manifest, part.length(), part.length(), PackDownloadState.VERIFYING))
        if (part.length() < manifest.sizeBytes * 0.95 && manifest.sizeBytes > 0) {
            throw IllegalStateException("The download ended early. It will resume next time.")
        }
        if (part.renameTo(destination)) destination else part
    }

    private fun progress(
        manifest: PackManifest,
        downloaded: Long,
        total: Long,
        state: PackDownloadState,
    ) = PackDownloadProgress(
        packId = manifest.id,
        state = state,
        bytesDownloaded = downloaded,
        totalBytes = total.coerceAtLeast(downloaded),
        resumable = true,
    )

    private fun output(file: File): File = file

    fun verify(file: File, sha256: String): Boolean = Crypto.sha256Hex(file.readBytes()) == sha256
}
