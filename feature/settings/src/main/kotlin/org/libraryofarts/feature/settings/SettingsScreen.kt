package org.libraryofarts.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.libraryofarts.core.common.BuildInfo
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.datastore.ImageQuality
import org.libraryofarts.core.datastore.LanguageOption
import org.libraryofarts.core.datastore.ThemeMode
import org.libraryofarts.core.datastore.UserPreferences
import org.libraryofarts.core.model.InstalledPack
import org.libraryofarts.core.model.PackDownloadProgress
import org.libraryofarts.core.model.PackManifest
import org.libraryofarts.core.ui.ScreenTitle

private enum class BackupMode { EXPORT, RESTORE }

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    val installedBytes by viewModel.installedBytes.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var backupMode by remember { mutableStateOf<BackupMode?>(null) }
    var pendingPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val pass = pendingPassphrase
        pendingPassphrase = null
        if (uri != null && pass != null) viewModel.exportBackup(uri, pass)
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val pass = pendingPassphrase
        pendingPassphrase = null
        if (uri != null && pass != null) viewModel.restoreBackup(uri, pass)
    }

    SoftBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp),
        ) {
            item("title") {
                ScreenTitle("Settings", "A quiet library: no account, no ads, no tracking.")
            }
            item("appearance") {
                SettingsSection("Appearance") {
                    PreferenceChoices(
                        title = "Theme",
                        options = ThemeMode.entries.map { it.label },
                        selected = prefs.themeMode.label,
                        onSelect = { label -> viewModel.setTheme(ThemeMode.entries.first { it.label == label }) },
                    )
                    SwitchRow(
                        title = "Pure black",
                        subtitle = "True black surfaces for AMOLED screens",
                        checked = prefs.pureBlack,
                        onCheckedChange = viewModel::setPureBlack,
                    )
                    SwitchRow(
                        title = "Material You",
                        subtitle = "Use wallpaper colours (Android 12+)",
                        checked = prefs.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                    SliderRow(
                        title = "Text size",
                        valueLabel = "${(prefs.fontScale * 100).toInt()}%",
                        value = prefs.fontScale,
                        range = 0.85f..1.5f,
                        onChange = viewModel::setFontScale,
                    )
                    TextButton(onClick = { viewModel.setUseSystemFontScale(!prefs.useSystemFontScale) }) {
                        Text(if (prefs.useSystemFontScale) "Following the system font size" else "Using the library's own font size")
                    }
                }
            }
            item("reading") {
                SettingsSection("Reading & images") {
                    PreferenceChoices(
                        title = "Image quality",
                        options = ImageQuality.entries.map { it.label },
                        selected = prefs.imageQuality.label,
                        onSelect = { label -> viewModel.setImageQuality(ImageQuality.entries.first { it.label == label }) },
                    )
                    SwitchRow("Data saver", "Prefer smaller images and avoid preloading", prefs.dataSaver, viewModel::setDataSaver)
                    SwitchRow("Wi-Fi only for packs", "Never spend mobile data on art packs", prefs.downloadOverWifiOnly, viewModel::setWifiOnly)
                    SwitchRow("Reduce mobile data", "Smaller images when not on Wi-Fi", prefs.reduceDataOnMobile, viewModel::setReduceDataOnMobile)
                    SwitchRow("Reduced motion", "Fade instead of spring animations", prefs.reducedMotion, viewModel::setReducedMotion)
                    SwitchRow("High contrast", "Stronger borders and text contrast", prefs.highContrast, viewModel::setHighContrast)
                    SwitchRow("Colour-blind-safe labels", "Patterns and labels alongside colour", prefs.colorBlindSafe, viewModel::setColorBlindSafe)
                    SwitchRow("Haptic feedback", "Gentle taps and confirms", prefs.compareHaptics, viewModel::setCompareHaptics)
                }
            }
            item("notifications") {
                SettingsSection("Daily artwork") {
                    SwitchRow("One quiet notification a day", "A single work, no streak shaming", prefs.notificationsEnabled, viewModel::setNotifications)
                    Text(
                        "Delivery around ${twoDigits(prefs.notificationHour)}:${twoDigits(prefs.notificationMinute)}, quiet ${prefs.quietStartHour}:00–${prefs.quietEndHour}:00.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    PreferenceChoices(
                        title = "Preferred time",
                        options = listOf("07:00", "08:00", "09:00", "12:00", "18:00", "20:00"),
                        selected = "${twoDigits(prefs.notificationHour)}:${twoDigits(prefs.notificationMinute)}",
                        onSelect = { value ->
                            val (hour, minute) = value.split(":").map { it.toInt() }
                            viewModel.setNotificationTime(hour, minute)
                        },
                    )
                    Text(
                        "Notifications are scheduled on-device with WorkManager. Nothing is sent to a server.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item("language") {
                SettingsSection("Language & script") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LanguageOption.entries, key = { it.tag }) { language ->
                            LibraryPill(
                                language.label,
                                selected = prefs.languageTag == language.tag,
                                onClick = { viewModel.setLanguage(language.tag) },
                            )
                        }
                    }
                    Text(
                        "Right-to-left scripts are laid out automatically. Some descriptions remain in the language supplied by the museum.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item("learning") {
                SettingsSection("Your Eyes First & learning") {
                    SwitchRow("On-device taste learning", "Personalise rows from what you linger on", prefs.tasteLearningEnabled, viewModel::setTasteEnabled)
                    SliderRow(
                        title = "Discovery reserve",
                        valueLabel = "${(prefs.discoveryRatio * 100).toInt()}% unfamiliar",
                        value = prefs.discoveryRatio,
                        range = 0f..0.6f,
                        onChange = viewModel::setDiscoveryRatio,
                    )
                    SwitchRow("On-device interpretation model", "Use the local model when installed; otherwise a rule-based reading", prefs.llmEnabled, viewModel::setLlmEnabled)
                    SwitchRow("Autoplay audio", "Start narration when a work opens", prefs.audioAutoplay, viewModel::setAudioAutoplay)
                    SwitchRow("Kids Mode", "Bigger pictures, simpler words", prefs.kidsMode, viewModel::setKidsMode)
                    SwitchRow("Lock the Journal", "Require device authentication", prefs.journalLockEnabled, viewModel::setJournalLock)
                    SwitchRow("Search history", "Keep recent searches on this device", prefs.searchHistoryEnabled, viewModel::setSearchHistory)
                }
            }
            item("packs-header") {
                SectionHeader("Offline packs", subtitle = "Download once, look forever. ${formatBytes(installedBytes)} installed.")
            }
            item("packs-note") {
                Text(
                    "Packs are verified with SHA-256 before any file is unpacked. Zip-slip paths are rejected. Archives are served from the project's public release downloads.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            if (installed.isNotEmpty()) {
                items(installed, key = { "installed-${it.id}" }) { pack ->
                    InstalledPackCard(pack, onDelete = { viewModel.deletePack(pack) })
                }
            }
            items(viewModel.catalog, key = { "catalog-${it.id}" }) { manifest ->
                CatalogPackCard(
                    manifest = manifest,
                    installed = installed.any { it.id == manifest.id },
                    progress = progress[manifest.id],
                    freeBytes = viewModel.availableBytes,
                    onInstall = { viewModel.downloadPack(manifest) },
                )
            }
            item("storage") {
                SettingsSection("Storage") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Image cache · ${formatBytes(cacheBytes)}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Installed packs · ${formatBytes(installedBytes)} · ${formatBytes(viewModel.availableBytes)} free",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArtButton("Clear image cache", onClick = viewModel::clearCache, filled = false)
                        ArtButton("Recalculate", onClick = viewModel::refreshCacheSize, filled = false)
                    }
                    Text(
                        "Clearing the cache never removes saved works, collections, the journal or installed packs.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            item("backup") {
                SettingsSection("Encrypted backup") {
                    Text(
                        "Export a single encrypted file of your saved works, journal, collections and flashcards. The passphrase is never stored; without it the file cannot be read.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArtButton("Export backup", onClick = { backupMode = BackupMode.EXPORT }, icon = Icons.Default.Lock)
                        ArtButton("Restore", onClick = { backupMode = BackupMode.RESTORE }, filled = false)
                    }
                }
            }
            item("privacy") {
                SettingsSection("Privacy") {
                    Text(
                        "No account. No advertising SDK. No analytics SDK. Crash reports are opt-in and contain no artwork, journal text or location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SwitchRow("Opt-in crash reporting", "Off by default; can be changed at any time", prefs.crashReportingOptIn, viewModel::setCrashOptIn)
                    TextButton(onClick = { showPrivacy = true }) { Text("Open privacy dashboard") }
                }
            }
            item("about") {
                SettingsSection("About") {
                    Text(BuildInfo.APP_NAME, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A non-commercial public-domain art library. Artwork metadata belongs to the holding museums; images retain the licence stated on each record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Version 1.0.3 · GPLv3 · Made for slow looking", style = MaterialTheme.typography.labelSmall)
                    Text("Privacy questions: ${BuildInfo.PRIVACY_EMAIL}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (backupMode != null) {
        BackupDialog(
            mode = backupMode!!,
            onDismiss = { backupMode = null },
            onConfirm = { passphrase ->
                val mode = backupMode
                pendingPassphrase = passphrase
                backupMode = null
                when (mode) {
                    BackupMode.EXPORT -> exportLauncher.launch(viewModel.suggestBackupName())
                    BackupMode.RESTORE -> restoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    null -> Unit
                }
            },
        )
    }

    if (showPrivacy) {
        PrivacyDialog(prefs = prefs, installedCount = installed.size, onDismiss = { showPrivacy = false })
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("OK") } },
            text = { Text(text) },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(7.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PreferenceChoices(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { option ->
            LibraryPill(option, selected = option == selected, onClick = { onSelect(option) })
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        valueRange = range,
    )
}

@Composable
private fun InstalledPackCard(pack: InstalledPack, onDelete: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pack.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "v${pack.version} · ${pack.artworkCount} works · ${pack.sizeLabel} · ${if (pack.checksumVerified) "checksum ✓" else "checksum —"}" +
                            if (pack.signatureVerified) " · signature ✓" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove ${pack.title}")
                }
            }
        }
    }
}

@Composable
private fun CatalogPackCard(
    manifest: PackManifest,
    installed: Boolean,
    progress: PackDownloadProgress?,
    freeBytes: Long,
    onInstall: () -> Unit,
) {
    val published = manifest.sha256.any { it != '0' }
    val downloading = progress != null && progress.state in setOf(
        org.libraryofarts.core.model.PackDownloadState.QUEUED,
        org.libraryofarts.core.model.PackDownloadState.DOWNLOADING,
        org.libraryofarts.core.model.PackDownloadState.VERIFYING,
        org.libraryofarts.core.model.PackDownloadState.INSTALLING,
    )
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(manifest.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${manifest.artworkCount} works · ${formatBytes(manifest.sizeBytes)} · ${manifest.qualityLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (installed) {
                    Text("Installed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                } else if (published) {
                    ArtButton("Install", onClick = onInstall, icon = Icons.Default.Download)
                } else {
                    Text("Not published", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                manifest.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
                Text(
                    progress.error ?: "${progress.state.name.lowercase()} · ${formatBytes(progress.bytesDownloaded)} of ${formatBytes(progress.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            if (!published) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "The catalogue is wired to your own CDN. Publish a signed archive and SHA-256 to enable this pack. Free space: ${formatBytes(freeBytes)}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackupDialog(mode: BackupMode, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = passphrase.length >= 8 && (mode == BackupMode.RESTORE || passphrase == confirm)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == BackupMode.EXPORT) "Encrypt this backup" else "Restore a backup") },
        text = {
            Column {
                Text(
                    if (mode == BackupMode.EXPORT) {
                        "Choose a passphrase of at least 8 characters. It is never stored, and cannot be recovered."
                    } else {
                        "Enter the passphrase used when this backup was exported."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (mode == BackupMode.EXPORT) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Repeat passphrase") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(passphrase.toCharArray()) },
                enabled = valid,
            ) { Text(if (mode == BackupMode.EXPORT) "Choose file" else "Choose file") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PrivacyDialog(prefs: UserPreferences, installedCount: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy dashboard") },
        text = {
            Column {
                PrivacyLine("Museum requests", "Only to the museums you open; no identifiers attached.")
                PrivacyLine("Taste profile", "Stored on this device, ${if (prefs.tasteLearningEnabled) "learning on" else "paused"}.")
                PrivacyLine("Journal", "Encrypted at rest; ${if (prefs.journalLockEnabled) "device lock enabled" else "device lock off"}.")
                PrivacyLine("Location", "Requested only in Museum Visit; never stored or shared.")
                PrivacyLine("Crash reports", if (prefs.crashReportingOptIn) "Opted in, with no personal content." else "Off.")
                PrivacyLine("Offline packs", "$installedCount installed; verified before extraction.")
                PrivacyLine("Accounts & ads", "None. There is nothing to sell and no one to sell it to.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun PrivacyLine(title: String, detail: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
