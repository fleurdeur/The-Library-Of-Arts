package org.libraryofarts.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A printed-catalogue palette: warm paper, near-black ink, vermilion for the
 * single accent, moss and brass for secondary information. There are no glass
 * panels or decorative gradients in the interface; hierarchy comes from type,
 * hairlines and generous margins.
 */
object LibraryColors {
    val Vermilion = Color(0xFFA63C26)
    val VermilionLight = Color(0xFFE8876C)
    val Ochre = Color(0xFF946B2D)
    val Ink = Color(0xFF171512)
    val Paper = Color(0xFFF3EFE6)
    val Parchment = Color(0xFFE9E2D4)
    val Sage = Color(0xFF59634B)
    val Slate = Color(0xFF565B5E)
    val GoldLeaf = Color(0xFFB08A4A)
    val Night = Color(0xFF11100E)
    val NightSurface = Color(0xFF181613)
    val NightCard = Color(0xFF211E1A)
    val AmoledBlack = Color(0xFF000000)
    val Error = Color(0xFF9E2A20)
    val Success = Color(0xFF3E5F43)
    val Warning = Color(0xFF8A5A18)

    val accentSeeds = listOf(
        "Vermilion" to Vermilion,
        "Ochre" to Ochre,
        "Sage" to Sage,
        "Slate" to Slate,
        "Gold leaf" to GoldLeaf,
        "Indigo" to Color(0xFF35425F),
        "Rose madder" to Color(0xFF83364E),
        "Prussian blue" to Color(0xFF26445F),
    )

    fun light(accent: Color = Vermilion, highContrast: Boolean = false) = lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = if (highContrast) accent else Color(0xFFF0D8CD),
        onPrimaryContainer = if (highContrast) Color.White else Color(0xFF42170D),
        secondary = Sage,
        onSecondary = Color.White,
        secondaryContainer = if (highContrast) Color(0xFFE2E6DA) else Color(0xFFE2E6DA),
        onSecondaryContainer = Ink,
        tertiary = Ochre,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF2E4C8),
        onTertiaryContainer = Color(0xFF3E2B05),
        background = if (highContrast) Color.White else Paper,
        onBackground = if (highContrast) Color.Black else Ink,
        surface = if (highContrast) Color.White else Color(0xFFFAF7F1),
        onSurface = if (highContrast) Color.Black else Ink,
        surfaceVariant = if (highContrast) Color(0xFFEDEDED) else Parchment,
        onSurfaceVariant = if (highContrast) Color.Black else Color(0xFF5E574C),
        outline = if (highContrast) Color.Black else Color(0xFF8A8174),
        outlineVariant = if (highContrast) Color.Black else Color(0xFFD8D0C1),
        error = Error,
        onError = Color.White,
        surfaceTint = accent,
    )

    fun dark(accent: Color = VermilionLight, pureBlack: Boolean = false, highContrast: Boolean = false) = darkColorScheme(
        primary = if (highContrast) Color(0xFFFFB4A2) else accent,
        onPrimary = Color(0xFF3A0B00),
        primaryContainer = if (highContrast) Color(0xFF7A2A18) else Color(0xFF4A1A10),
        onPrimaryContainer = if (highContrast) Color.White else Color(0xFFFFDAD2),
        secondary = Color(0xFFB8C4A9),
        onSecondary = Color(0xFF24301D),
        secondaryContainer = Color(0xFF36412D),
        onSecondaryContainer = Color(0xFFD4E0C5),
        tertiary = Color(0xFFD8B878),
        onTertiary = Color(0xFF3D2E08),
        tertiaryContainer = Color(0xFF58451A),
        onTertiaryContainer = Color(0xFFF6E2B8),
        background = when {
            pureBlack -> AmoledBlack
            highContrast -> Color.Black
            else -> Night
        },
        onBackground = if (highContrast) Color.White else Color(0xFFECE5DA),
        surface = when {
            pureBlack -> AmoledBlack
            highContrast -> Color.Black
            else -> NightSurface
        },
        onSurface = if (highContrast) Color.White else Color(0xFFECE5DA),
        surfaceVariant = if (pureBlack) Color(0xFF0B0B0B) else NightCard,
        onSurfaceVariant = if (highContrast) Color.White else Color(0xFFC9C0B4),
        outline = if (highContrast) Color.White else Color(0xFF968D81),
        outlineVariant = if (highContrast) Color.White else Color(0xFF3C3731),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        surfaceTint = accent,
    )
}

/** Daltonism-aware palette: hues spaced for the most common colour vision types. */
val colorBlindSafePalette = listOf(
    Color(0xFF0072B2),
    Color(0xFFE69F00),
    Color(0xFF009E73),
    Color(0xFFCC79A7),
    Color(0xFF56B4E9),
    Color(0xFFD55E00),
    Color(0xFFF0E442),
)
