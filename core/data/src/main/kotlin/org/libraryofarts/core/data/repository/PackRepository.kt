package org.libraryofarts.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.libraryofarts.core.data.mapper.ArtworkJson
import org.libraryofarts.core.data.pack.PackDownloader
import org.libraryofarts.core.data.pack.PackInstaller
import org.libraryofarts.core.database.ArtworkDao
import org.libraryofarts.core.database.PackDao
import org.libraryofarts.core.model.InstalledPack
import org.libraryofarts.core.model.PackDownloadProgress
import org.libraryofarts.core.model.PackDownloadState
import org.libraryofarts.core.model.PackManifest
import org.libraryofarts.core.model.PackQuality
import java.util.UUID

@Singleton
class PackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packDao: PackDao,
    private val artworkDao: ArtworkDao,
    private val downloader: PackDownloader,
    private val installer: PackInstaller,
    private val trust: PackTrust,
) {
    private val _progress = MutableStateFlow<Map<String, PackDownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, PackDownloadProgress>> = _progress.asStateFlow()

    val installed: Flow<List<InstalledPack>> = packDao.observeInstalled().map { list ->
        list.map { it.toModel() }
    }

    val totalInstalledBytes: Flow<Long> = packDao.observeTotalBytes()

    fun availableBytes(): Long = context.filesDir.usableSpace

    fun canInstall(manifest: PackManifest): Boolean =
        availableBytes() > manifest.sizeBytes + 128L * 1024 * 1024

    suspend fun downloadAndInstall(manifest: PackManifest): Result<InstalledPack> {
        update(manifest.id, PackDownloadProgress(manifest.id, PackDownloadState.QUEUED))
        return runCatching {
            val archive = downloader.download(manifest) { progress ->
                update(manifest.id, progress)
            }
            installer.install(
                archive = archive,
                manifest = manifest,
                publicKeyBase64 = trust.publicKeyBase64.takeIf { it.isNotBlank() },
                requireSignature = trust.requireSignature,
                onProgress = { state ->
                    update(manifest.id, PackDownloadProgress(manifest.id, state))
                },
            ).getOrThrow()
        }.onFailure { error ->
            update(
                manifest.id,
                PackDownloadProgress(
                    packId = manifest.id,
                    state = PackDownloadState.FAILED,
                    error = error.message ?: "The pack could not be installed.",
                ),
            )
        }
    }

    suspend fun installFromArchive(archive: File, manifest: PackManifest): Result<InstalledPack> =
        installer.install(
            archive = archive,
            manifest = manifest,
            publicKeyBase64 = trust.publicKeyBase64.takeIf { it.isNotBlank() },
            requireSignature = trust.requireSignature,
            onProgress = { state ->
                update(manifest.id, PackDownloadProgress(manifest.id, state))
            },
        )

    suspend fun delete(packId: String) {
        packDao.getById(packId)?.let { entity ->
            runCatching { File(entity.localPath).deleteRecursively() }
        }
        artworkDao.deletePackArtworks(packId)
        packDao.delete(packId)
        _progress.value = _progress.value - packId
    }

    suspend fun isInstalled(packId: String): Boolean = packDao.getById(packId) != null

    suspend fun installedPack(packId: String): InstalledPack? = packDao.getById(packId)?.toModel()

    fun clearError(packId: String) = update(packId, null)

    private fun update(packId: String, progress: PackDownloadProgress?) {
        _progress.value = if (progress == null) {
            _progress.value - packId
        } else {
            _progress.value + (packId to progress)
        }
    }

    private fun org.libraryofarts.core.database.InstalledPackEntity.toModel(): InstalledPack = InstalledPack(
        id = id,
        title = title,
        version = version,
        period = period,
        genre = genre,
        quality = runCatching { PackQuality.valueOf(quality) }.getOrDefault(PackQuality.STANDARD),
        artworkCount = artworkCount,
        sizeBytes = sizeBytes,
        installedAt = installedAt,
        localPath = localPath,
        checksumVerified = checksumVerified,
        signatureVerified = signatureVerified,
        deltaApplied = deltaApplied,
    )
}

data class PackTrust(
    val publicKeyBase64: String = "",
    val requireSignature: Boolean = false,
)
