package org.libraryofarts.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class LibraryThemeState(
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val colorBlindSafe: Boolean = false,
)

val LocalLibraryThemeState = staticCompositionLocalOf { LibraryThemeState() }

@Composable
fun LibraryOfArtsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    dynamicColor: Boolean = false,
    accent: Color? = null,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    colorBlindSafe: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LibraryColors.dark(accent ?: LibraryColors.VermilionLight, pureBlack, highContrast)
        else -> LibraryColors.light(accent ?: LibraryColors.Vermilion, highContrast)
    }

    CompositionLocalProvider(
        LocalLibraryThemeState provides LibraryThemeState(highContrast, reducedMotion, colorBlindSafe),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = LibraryTypography,
            shapes = LibraryShapes,
            content = content,
        )
    }
}
