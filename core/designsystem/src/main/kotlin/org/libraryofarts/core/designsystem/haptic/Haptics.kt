package org.libraryofarts.core.designsystem.haptic

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Small, restrained haptics. Respects the system's haptic setting; the view
 * simply does nothing when haptics are disabled by the user.
 */
class LibraryHaptics(private val view: android.view.View?) {
    fun tick() {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun confirm() {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun longPress() {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
fun rememberLibraryHaptics(): LibraryHaptics {
    val view = LocalView.current
    return remember(view) { LibraryHaptics(view) }
}
