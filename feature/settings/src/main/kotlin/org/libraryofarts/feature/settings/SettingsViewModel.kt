package org.libraryofarts.feature.settings

import android.content.Context
import android.net.Uri
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libraryofarts.core.data.export.BackupRepository
import org.libraryofarts.core.data.pack.PackCatalog
import org.libraryofarts.core.data.repository.PackRepository
import org.libraryofarts.core.data.update.AppUpdate
import org.libraryofarts.core.data.update.UpdateAsset
import org.libraryofarts.core.data.update.UpdateCheck
import org.libraryofarts.core.data.update.UpdateDownloadProgress
import org.libraryofarts.core.data.update.UpdateInstallResult
import org.libraryofarts.core.data.update.UpdateRepository
import org.libraryofarts.core.common.BuildInfo
import org.libraryofarts.core.datastore.ImageQuality
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.datastore.ThemeMode
import org.libraryofarts.core.datastore.UserPreferences
import org.libraryofarts.core.model.InstalledPack
import org.libraryofarts.core.model.PackDownloadProgress
import org.libraryofarts.core.model.PackManifest

data class UpdateUiState(
    val checking: Boolean = false,
    val check: UpdateCheck? = null,
    val download: UpdateDownloadProgress = UpdateDownloadProgress(),
    val downloaded: File? = null,
    val downloadedVersion: String? = null,
    val permissionRequired: Boolean = false,
    val currentVersion: String = BuildInfo.VERSION_NAME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val packs: PackRepository,
    private val backup: BackupRepository,
    private val updates: UpdateRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settings.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val installed: StateFlow<List<InstalledPack>> = packs.installed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val installedBytes: StateFlow<Long> = packs.totalInstalledBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val progress: StateFlow<Map<String, PackDownloadProgress>> = packs.progress

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _update = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _update

    val catalog: List<PackManifest> = PackCatalog.packs
    val availableBytes: Long get() = packs.availableBytes()

    init {
        refreshCacheSize()
        updates.cleanUpIfInstalled()
        checkForUpdates()
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun checkForUpdates(force: Boolean = true) {
        if (_update.value.checking || _update.value.download.state == UpdateDownloadProgress.State.DOWNLOADING) return
        _update.update { it.copy(checking = true, permissionRequired = false) }
        viewModelScope.launch {
            val result = updates.check(BuildInfo.VERSION_NAME, force = force)
            val ready = updates.readyDownload()
            _update.update {
                it.copy(
                    checking = false,
                    check = result,
                    downloaded = ready,
                    downloadedVersion = updates.readyVersion(),
                    download = if (ready != null) {
                        UpdateDownloadProgress(ready.length(), ready.length(), UpdateDownloadProgress.State.READY)
                    } else {
                        it.download
                    },
                )
            }
        }
    }

    fun downloadUpdate(asset: UpdateAsset) {
        val version = _update.value.check?.latest?.versionName ?: return
        if (_update.value.download.state == UpdateDownloadProgress.State.DOWNLOADING) return
        viewModelScope.launch {
            updates.download(asset) { progress ->
                _update.update { it.copy(download = progress) }
            }.onSuccess { file ->
                updates.rememberReady(asset, version)
                _update.update {
                    it.copy(
                        downloaded = file,
                        downloadedVersion = version,
                        download = UpdateDownloadProgress(file.length(), file.length(), UpdateDownloadProgress.State.READY),
                    )
                }
                _message.value = "Downloaded ${asset.architecture.label}. Tap Install when you are ready."
            }.onFailure { error ->
                _message.value = error.message ?: "The update could not be downloaded."
            }
        }
    }

    fun installDownloaded() {
        val state = _update.value
        val file = state.downloaded ?: return
        when (val result = updates.install(file, state.downloadedVersion ?: "")) {
            is UpdateInstallResult.Started -> {
                _update.update { it.copy(permissionRequired = false) }
                _message.value = "Installer opened. Your library, packs and journal stay in place."
            }
            UpdateInstallResult.PermissionRequired -> {
                _update.update { it.copy(permissionRequired = true) }
            }
            is UpdateInstallResult.Failed -> _message.value = result.message
        }
    }

    fun installPermissionIntent(): Intent = updates.permissionIntent()

    fun dismissInstallPermission() {
        _update.update { it.copy(permissionRequired = false) }
    }

    fun clearDownloadedUpdate() {
        updates.clearAllDownloads()
        _update.update {
            it.copy(
                downloaded = null,
                downloadedVersion = null,
                download = UpdateDownloadProgress(),
            )
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheBytes.value = withContext(Dispatchers.IO) { directorySize(context.cacheDir) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            }
            refreshCacheSize()
            _message.value = "Image and temporary caches cleared. Your saved works and journal were not touched."
        }
    }

    fun setTheme(mode: ThemeMode) = launchSetting { settings.setThemeMode(mode) }
    fun setPureBlack(value: Boolean) = launchSetting { settings.setPureBlack(value) }
    fun setDynamicColor(value: Boolean) = launchSetting { settings.setDynamicColor(value) }
    fun setImageQuality(value: ImageQuality) = launchSetting { settings.setImageQuality(value) }
    fun setDataSaver(value: Boolean) = launchSetting { settings.setDataSaver(value) }
    fun setWifiOnly(value: Boolean) = launchSetting { settings.setWifiOnly(value) }
    fun setReduceDataOnMobile(value: Boolean) = launchSetting { settings.setReduceDataOnMobile(value) }
    fun setNotifications(value: Boolean) = launchSetting { settings.setNotificationsEnabled(value) }
    fun setQuietHours(start: Int, end: Int) = launchSetting { settings.setQuietHours(start, end) }
    fun setNotificationTime(hour: Int, minute: Int) = launchSetting { settings.setNotificationTime(hour, minute) }
    fun setLanguage(tag: String) = launchSetting { settings.setLanguage(tag) }
    fun setKidsMode(value: Boolean) = launchSetting { settings.setKidsMode(value) }
    fun setHighContrast(value: Boolean) = launchSetting { settings.setHighContrast(value) }
    fun setReducedMotion(value: Boolean) = launchSetting { settings.setReducedMotion(value) }
    fun setColorBlindSafe(value: Boolean) = launchSetting { settings.setColorBlindSafe(value) }
    fun setUseSystemFontScale(value: Boolean) = launchSetting { settings.setUseSystemFontScale(value) }
    fun setFontScale(value: Float) = launchSetting { settings.setFontScale(value) }
    fun setTasteEnabled(value: Boolean) = launchSetting { settings.setTasteLearningEnabled(value) }
    fun setDiscoveryRatio(value: Float) = launchSetting { settings.setDiscoveryRatio(value) }
    fun setLlmEnabled(value: Boolean) = launchSetting { settings.setLlmEnabled(value) }
    fun setJournalLock(value: Boolean) = launchSetting { settings.setJournalLock(value) }
    fun setAudioAutoplay(value: Boolean) = launchSetting { settings.setAudioAutoplay(value) }
    fun setCrashOptIn(value: Boolean) = launchSetting { settings.setCrashReportingOptIn(value) }
    fun setCompareHaptics(value: Boolean) = launchSetting { settings.setCompareHaptics(value) }
    fun setSearchHistory(value: Boolean) = launchSetting { settings.setSearchHistoryEnabled(value) }

    fun downloadPack(manifest: PackManifest) {
        if (!packs.canInstall(manifest)) {
            _message.value = "Not enough free space. ${manifest.title} needs about ${formatBytes(manifest.sizeBytes)} plus working room."
            return
        }
        viewModelScope.launch {
            val result = packs.downloadAndInstall(manifest)
            result.onSuccess {
                _message.value = "${it.title} installed and indexed. It is ready offline."
            }.onFailure {
                _message.value = it.message ?: "The pack could not be installed."
            }
        }
    }

    fun deletePack(pack: InstalledPack) {
        viewModelScope.launch {
            packs.delete(pack.id)
            _message.value = "${pack.title} removed from this device."
            refreshCacheSize()
        }
    }

    fun exportBackup(uri: Uri, passphrase: CharArray) {
        viewModelScope.launch {
            backup.exportTo(uri, passphrase)
                .onSuccess { count -> _message.value = "Encrypted backup written ($count records). Keep the passphrase safe — it cannot be recovered." }
                .onFailure { _message.value = it.message ?: "The backup could not be written." }
            passphrase.fill('\u0000')
        }
    }

    fun restoreBackup(uri: Uri, passphrase: CharArray) {
        viewModelScope.launch {
            backup.restoreFrom(uri, passphrase)
                .onSuccess { count -> _message.value = "Backup restored ($count records merged into your library)." }
                .onFailure { _message.value = it.message ?: "The backup could not be read. Check the passphrase and file." }
            passphrase.fill('\u0000')
        }
    }

    fun suggestBackupName(): String = backup.suggestFileName()

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }

    private fun directorySize(file: File): Long {
        if (!file.exists()) return 0
        return file.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
