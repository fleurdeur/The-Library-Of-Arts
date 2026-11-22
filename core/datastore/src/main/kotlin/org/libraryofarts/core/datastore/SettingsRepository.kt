package org.libraryofarts.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "library_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val themeMode = stringPreferencesKey("theme_mode")
        val pureBlack = booleanPreferencesKey("pure_black")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val accent = stringPreferencesKey("accent")
        val fontScale = floatPreferencesKey("font_scale")
        val systemFontScale = booleanPreferencesKey("system_font_scale")
        val highContrast = booleanPreferencesKey("high_contrast")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
        val colorBlind = booleanPreferencesKey("color_blind")
        val imageQuality = stringPreferencesKey("image_quality")
        val dataSaver = booleanPreferencesKey("data_saver")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val notifications = booleanPreferencesKey("notifications")
        val notifHour = intPreferencesKey("notif_hour")
        val notifMinute = intPreferencesKey("notif_minute")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val tasteEnabled = booleanPreferencesKey("taste_enabled")
        val discovery = floatPreferencesKey("discovery_ratio")
        val kids = booleanPreferencesKey("kids_mode")
        val language = stringPreferencesKey("language")
        val crashOptIn = booleanPreferencesKey("crash_opt_in")
        val lastNotifArtwork = stringPreferencesKey("last_notif_artwork")
        val lastStreakDay = longPreferencesKey("last_streak_day")
        val currentStreak = intPreferencesKey("current_streak")
        val longestStreak = intPreferencesKey("longest_streak")
        val totalDays = intPreferencesKey("total_days")
        val audioAutoplay = booleanPreferencesKey("audio_autoplay")
        val llmEnabled = booleanPreferencesKey("llm_enabled")
        val journalLock = booleanPreferencesKey("journal_lock")
        val mobileData = booleanPreferencesKey("mobile_data")
        val showMap = booleanPreferencesKey("show_map")
        val compareHaptics = booleanPreferencesKey("compare_haptics")
        val searchHistory = booleanPreferencesKey("search_history")
        val installId = stringPreferencesKey("install_id")
    }

    val preferences: Flow<UserPreferences> = context.settingsDataStore.data.map { p ->
        UserPreferences(
            onboardingComplete = p[Keys.onboarding] ?: false,
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            pureBlack = p[Keys.pureBlack] ?: false,
            dynamicColor = p[Keys.dynamic] ?: false,
            accentName = p[Keys.accent] ?: "Vermilion",
            useSystemFontScale = p[Keys.systemFontScale] ?: true,
            fontScale = p[Keys.fontScale] ?: 1f,
            highContrast = p[Keys.highContrast] ?: false,
            reducedMotion = p[Keys.reducedMotion] ?: false,
            colorBlindSafe = p[Keys.colorBlind] ?: false,
            imageQuality = p[Keys.imageQuality]?.let { runCatching { ImageQuality.valueOf(it) }.getOrNull() } ?: ImageQuality.BALANCED,
            dataSaver = p[Keys.dataSaver] ?: false,
            downloadOverWifiOnly = p[Keys.wifiOnly] ?: true,
            notificationsEnabled = p[Keys.notifications] ?: true,
            notificationHour = p[Keys.notifHour] ?: 9,
            notificationMinute = p[Keys.notifMinute] ?: 0,
            quietStartHour = p[Keys.quietStart] ?: 22,
            quietEndHour = p[Keys.quietEnd] ?: 7,
            tasteLearningEnabled = p[Keys.tasteEnabled] ?: true,
            discoveryRatio = p[Keys.discovery] ?: 0.25f,
            kidsMode = p[Keys.kids] ?: false,
            languageTag = p[Keys.language] ?: "system",
            crashReportingOptIn = p[Keys.crashOptIn] ?: false,
            lastNotificationArtworkId = p[Keys.lastNotifArtwork],
            lastStreakDay = p[Keys.lastStreakDay] ?: 0,
            currentStreak = p[Keys.currentStreak] ?: 0,
            longestStreak = p[Keys.longestStreak] ?: 0,
            totalViewingDays = p[Keys.totalDays] ?: 0,
            audioAutoplay = p[Keys.audioAutoplay] ?: false,
            llmEnabled = p[Keys.llmEnabled] ?: true,
            journalLockEnabled = p[Keys.journalLock] ?: false,
            reduceDataOnMobile = p[Keys.mobileData] ?: true,
            showMuseumMap = p[Keys.showMap] ?: true,
            compareHaptics = p[Keys.compareHaptics] ?: true,
            searchHistoryEnabled = p[Keys.searchHistory] ?: true,
            installId = p[Keys.installId] ?: "",
        )
    }

    suspend fun setOnboardingComplete(value: Boolean) = edit { it[Keys.onboarding] = value }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.themeMode] = value.name }
    suspend fun setPureBlack(value: Boolean) = edit { it[Keys.pureBlack] = value }
    suspend fun setDynamicColor(value: Boolean) = edit { it[Keys.dynamic] = value }
    suspend fun setAccent(value: String) = edit { it[Keys.accent] = value }
    suspend fun setFontScale(value: Float) = edit { it[Keys.fontScale] = value.coerceIn(0.8f, 1.6f) }
    suspend fun setUseSystemFontScale(value: Boolean) = edit { it[Keys.systemFontScale] = value }
    suspend fun setHighContrast(value: Boolean) = edit { it[Keys.highContrast] = value }
    suspend fun setReducedMotion(value: Boolean) = edit { it[Keys.reducedMotion] = value }
    suspend fun setColorBlindSafe(value: Boolean) = edit { it[Keys.colorBlind] = value }
    suspend fun setImageQuality(value: ImageQuality) = edit { it[Keys.imageQuality] = value.name }
    suspend fun setDataSaver(value: Boolean) = edit { it[Keys.dataSaver] = value }
    suspend fun setWifiOnly(value: Boolean) = edit { it[Keys.wifiOnly] = value }
    suspend fun setNotificationsEnabled(value: Boolean) = edit { it[Keys.notifications] = value }
    suspend fun setNotificationTime(hour: Int, minute: Int) = edit {
        it[Keys.notifHour] = hour
        it[Keys.notifMinute] = minute
    }
    suspend fun setQuietHours(startHour: Int, endHour: Int) = edit {
        it[Keys.quietStart] = startHour
        it[Keys.quietEnd] = endHour
    }
    suspend fun setTasteLearningEnabled(value: Boolean) = edit { it[Keys.tasteEnabled] = value }
    suspend fun setDiscoveryRatio(value: Float) = edit { it[Keys.discovery] = value.coerceIn(0f, 0.6f) }
    suspend fun setKidsMode(value: Boolean) = edit { it[Keys.kids] = value }
    suspend fun setLanguage(tag: String) = edit { it[Keys.language] = tag }
    suspend fun setCrashReportingOptIn(value: Boolean) = edit { it[Keys.crashOptIn] = value }
    suspend fun setAudioAutoplay(value: Boolean) = edit { it[Keys.audioAutoplay] = value }
    suspend fun setLlmEnabled(value: Boolean) = edit { it[Keys.llmEnabled] = value }
    suspend fun setJournalLock(value: Boolean) = edit { it[Keys.journalLock] = value }
    suspend fun setReduceDataOnMobile(value: Boolean) = edit { it[Keys.mobileData] = value }
    suspend fun setShowMuseumMap(value: Boolean) = edit { it[Keys.showMap] = value }
    suspend fun setSearchHistoryEnabled(value: Boolean) = edit { it[Keys.searchHistory] = value }
    suspend fun setCompareHaptics(value: Boolean) = edit { it[Keys.compareHaptics] = value }

    suspend fun recordStreak(
        todayEpochDay: Long,
        streak: Int,
        longest: Int,
        totalDays: Int,
        artworkId: String,
    ) = edit {
        it[Keys.lastStreakDay] = todayEpochDay
        it[Keys.currentStreak] = streak
        it[Keys.longestStreak] = longest
        it[Keys.totalDays] = totalDays
        it[Keys.lastNotifArtwork] = artworkId
    }

    suspend fun setInstallId(id: String) = edit { it[Keys.installId] = id }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }
}
