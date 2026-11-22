package org.libraryofarts.core.datastore

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pureBlack: Boolean = false,
    val dynamicColor: Boolean = false,
    val accentName: String = "Vermilion",
    val useSystemFontScale: Boolean = true,
    val fontScale: Float = 1f,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val colorBlindSafe: Boolean = false,
    val imageQuality: ImageQuality = ImageQuality.BALANCED,
    val dataSaver: Boolean = false,
    val downloadOverWifiOnly: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val tasteLearningEnabled: Boolean = true,
    val discoveryRatio: Float = 0.25f,
    val kidsMode: Boolean = false,
    val languageTag: String = "system",
    val crashReportingOptIn: Boolean = false,
    val lastNotificationArtworkId: String? = null,
    val lastStreakDay: Long = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalViewingDays: Int = 0,
    val audioAutoplay: Boolean = false,
    val llmEnabled: Boolean = true,
    val journalLockEnabled: Boolean = false,
    val reduceDataOnMobile: Boolean = true,
    val showMuseumMap: Boolean = true,
    val compareHaptics: Boolean = true,
    val searchHistoryEnabled: Boolean = true,
    val installId: String = "",
) {
    val usesDarkTheme: Boolean?
        get() = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> null
        }
}

enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class ImageQuality(val label: String, val maxWidth: Int) {
    SAVER("Data saver (fastest)", 1_200),
    BALANCED("Balanced", 2_400),
    HIGH("High detail", 4_000),
    LOSSLESS("Original / lossless", 0),
}

enum class LanguageOption(val tag: String, val label: String) {
    SYSTEM("system", "System language"),
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    ITALIAN("it", "Italiano"),
    PORTUGUESE("pt", "Português"),
    ARABIC("ar", "العربية"),
    HEBREW("he", "עברית"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    CHINESE("zh", "中文"),
}
