package org.libraryofarts.feature.settings

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libraryofarts.core.data.export.BackupRepository
import org.libraryofarts.core.data.pack.PackCatalog
import org.libraryofarts.core.data.repository.PackRepository
import org.libraryofarts.core.datastore.ImageQuality
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.datastore.ThemeMode
import org.libraryofarts.core.datastore.UserPreferences
import org.libraryofarts.core.model.InstalledPack
import org.libraryofarts.core.model.PackDownloadProgress
import org.libraryofarts.core.model.PackManifest

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val packs: PackRepository,
    private val backup: BackupRepository,
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

    val catalog: List<PackManifest> = PackCatalog.packs
    val availableBytes: Long get() = packs.availableBytes()

    init {
        refreshCacheSize()
    }

    fun dismissMessage() {
        _message.value = null
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
