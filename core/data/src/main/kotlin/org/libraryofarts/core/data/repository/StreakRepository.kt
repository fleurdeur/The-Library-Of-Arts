package org.libraryofarts.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.model.StreakState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    val streak: Flow<StreakState> = settings.preferences.map {
        StreakState(
            currentStreak = it.currentStreak,
            longestStreak = it.longestStreak,
            lastViewedEpochDay = it.lastStreakDay,
            totalDays = it.totalViewingDays,
        )
    }

    /**
     * Records today without punishment. Missing a day resets the count to 1;
     * the app never displays a broken-streak message or a loss animation.
     */
    suspend fun recordViewing(artworkId: String): StreakState {
        val prefs = settings.preferences.first()
        val today = time.todayEpochDay()
        if (prefs.lastStreakDay == today) {
            return StreakState(prefs.currentStreak, prefs.longestStreak, today, prefs.totalViewingDays)
        }
        val next = if (prefs.lastStreakDay == today - 1) prefs.currentStreak + 1 else 1
        val longest = maxOf(prefs.longestStreak, next)
        val total = prefs.totalViewingDays + 1
        settings.recordStreak(today, next, longest, total, artworkId)
        return StreakState(next, longest, today, total)
    }

    fun gentleMessage(state: StreakState): String = when {
        state.currentStreak <= 0 -> "Your first visit is a beginning."
        state.currentStreak == 1 -> "One day of looking. That's enough."
        state.currentStreak < 7 -> "${state.currentStreak} days of looking. No pressure — come back when you want."
        state.currentStreak < 30 -> "${state.currentStreak} days. Your eye is settling into a rhythm."
        else -> "${state.currentStreak} days of curiosity. What a collection of moments."
    }
}
