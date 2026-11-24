package org.libraryofarts.core.data.pack

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libraryofarts.core.common.Crypto
import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.data.mapper.ArtworkJson
import org.libraryofarts.core.data.mapper.toEntity
import org.libraryofarts.core.database.ArtworkDao
import org.libraryofarts.core.database.InstalledPackEntity
import org.libraryofarts.core.database.PackDao
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.InstalledPack
import org.libraryofarts.core.model.PackManifest
import org.libraryofarts.core.model.PackQuality
import org.libraryofarts.core.model.PackDownloadProgress
import org.libraryofarts.core.model.PackDownloadState

/**
 * Downloads are resumable (HTTP Range), checksum-verified, optionally signed
 * (ECDSA P-256), zip-slip safe and index into the encrypted Room database.
 */
@Singleton
class PackInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artworkDao: ArtworkDao,
    private val packDao: PackDao,
    private val time: TimeProvider,
) {
    suspend fun verifyArchive(file: File, manifest: PackManifest, publicKeyBase64: String?): Pair<Boolean, Boolean> =
        withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val checksumOk = digest.digest().toHex().equals(manifest.sha256, ignoreCase = true)
            val signature = manifest.signature
            val signatureOk = signature != null &&
                publicKeyBase64 != null &&
                Crypto.verifyEcdsaSha256(
                    payload = manifest.copy(signature = null, signatureKeyId = null)
                        .let { ArtworkJson.encodeToString(PackManifest.serializer(), it).toByteArray() },
                    signatureBase64 = signature,
                    publicKeyBase64 = publicKeyBase64,
                )
            checksumOk to signatureOk
        }

    suspend fun install(
        archive: File,
        manifest: PackManifest,
        publicKeyBase64: String?,
        requireSignature: Boolean,
        onProgress: (PackDownloadState) -> Unit = {},
    ): Result<InstalledPack> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(PackDownloadState.VERIFYING)
            val free = context.filesDir.usableSpace
            if (free < manifest.sizeBytes + 64L * 1024 * 1024) {
                throw PackInstallException("There isn't enough free storage. Free some space and try again.")
            }
            val (checksumOk, signatureOk) = verifyArchive(archive, manifest, publicKeyBase64)
            if (!checksumOk) throw PackInstallException("The pack checksum does not match. It was not installed.")
            if (requireSignature && !signatureOk) {
                throw PackInstallException("The pack signature could not be verified. It was not installed.")
            }
            onProgress(PackDownloadState.INSTALLING)
            val target = File(context.filesDir, "packs/${manifest.id}-${manifest.version}")
            if (target.exists()) target.deleteRecursively()
            target.mkdirs()
            unpackSafely(archive, target, manifest)

            val indexFile = File(target, manifest.artworksPath)
            if (!indexFile.exists()) throw PackInstallException("The pack has no artwork index.")
            val artworks = indexFile.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.mapNotNull { line ->
                    runCatching { ArtworkJson.decodeFromString(Artwork.serializer(), line) }.getOrNull()
                }.toList()
            }
            if (artworks.isEmpty()) throw PackInstallException("The pack contains no readable artworks.")

            artworkDao.deletePackArtworks(manifest.id)
            // Some packs use .jpg, .jpeg or .png for the same source id. Match
            // by the file stem so every recorded plate resolves to its image.
            val imageFiles = File(target, manifest.imagesDir).listFiles()
                .orEmpty()
                .filter { it.isFile }
                .associateBy { it.nameWithoutExtension }
            artworkDao.upsertAll(
                artworks.map { artwork ->
                    val local = imageFiles[artwork.sourceId]
                        ?: File(target, "${manifest.imagesDir}/${artwork.sourceId}.jpg").takeIf { it.exists() }
                    artwork.copy(
                        packId = manifest.id,
                        image = artwork.image.copy(
                            localPath = local?.absolutePath,
                        ),
                    ).toEntity()
                },
            )
            val installed = InstalledPack(
                id = manifest.id,
                title = manifest.title,
                version = manifest.version,
                period = manifest.period,
                genre = manifest.genre,
                quality = manifest.quality,
                artworkCount = artworks.size,
                sizeBytes = archive.length(),
                installedAt = time.nowMillis(),
                localPath = target.absolutePath,
                checksumVerified = checksumOk,
                signatureVerified = signatureOk,
                deltaApplied = false,
            )
            packDao.upsert(installed.toEntity(manifest))
            indexFile.delete()
            archive.delete()
            onProgress(PackDownloadState.COMPLETE)
            installed
        }.onFailure { onProgress(PackDownloadState.FAILED) }
    }

    private fun unpackSafely(archive: File, target: File, manifest: PackManifest) {
        val root = target.canonicalFile
        var entries = 0
        var total = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries++
                if (entries > 200_000) throw PackInstallException("The pack contains too many files.")
                val name = entry.name
                if (name.startsWith("/") || name.contains("..") || name.contains('\\')) {
                    throw PackInstallException("The pack contains an unsafe file path.")
                }
                val outFile = File(root, name).canonicalFile
                if (!outFile.path.startsWith(root.path + File.separator)) {
                    throw PackInstallException("The pack tried to write outside its folder.")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > manifest.sizeBytes * 3 + 64L * 1024 * 1024) {
                                throw PackInstallException("The pack expands far beyond its declared size.")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun InstalledPack.toEntity(manifest: PackManifest): InstalledPackEntity = InstalledPackEntity(
        id = id,
        title = title,
        version = version,
        period = period,
        genre = genre,
        quality = quality.name,
        artworkCount = artworkCount,
        sizeBytes = sizeBytes,
        installedAt = installedAt,
        localPath = localPath,
        checksumVerified = checksumVerified,
        signatureVerified = signatureVerified,
        deltaApplied = deltaApplied,
        manifestJson = ArtworkJson.encodeToString(PackManifest.serializer(), manifest),
    )

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}

class PackInstallException(message: String) : Exception(message)
