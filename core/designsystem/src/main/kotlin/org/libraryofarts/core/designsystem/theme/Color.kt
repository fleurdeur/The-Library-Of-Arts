package org.libraryofarts.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Warm gallery neutrals with a vermilion accent — a modern, quiet museum feel. */
object LibraryColors {
    val Vermilion = Color(0xFFB5452B)
    val VermilionLight = Color(0xFFE07A5F)
    val Ochre = Color(0xFFC59A48)
    val Ink = Color(0xFF1A1714)
    val Paper = Color(0xFFF7F2E9)
    val Parchment = Color(0xFFEFE7D8)
    val Sage = Color(0xFF6E7F6A)
    val Slate = Color(0xFF4A5056)
    val GoldLeaf = Color(0xFFD9B36C)
    val Night = Color(0xFF121110)
    val NightSurface = Color(0xFF1C1A18)
    val NightCard = Color(0xFF24211E)
    val AmoledBlack = Color(0xFF000000)
    val Error = Color(0xFFBA1A1A)
    val Success = Color(0xFF3E6B4F)
    val Warning = Color(0xFF9A6A1E)

    val accentSeeds = listOf(
        "Vermilion" to Vermilion,
        "Ochre" to Ochre,
        "Sage" to Sage,
        "Slate" to Slate,
        "Gold leaf" to GoldLeaf,
        "Indigo" to Color(0xFF3E4A7A),
        "Rose madder" to Color(0xFF9A3B5A),
        "Prussian blue" to Color(0xFF2B4C6F),
    )

    fun light(accent: Color = Vermilion, highContrast: Boolean = false) = lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = if (highContrast) 0.30f else 0.14f),
        onPrimaryContainer = if (highContrast) Color.Black else Ink,
        secondary = Ochre,
        onSecondary = Ink,
        secondaryContainer = Parchment,
        onSecondaryContainer = Ink,
        tertiary = Sage,
        onTertiary = Color.White,
        background = if (highContrast) Color.White else Paper,
        onBackground = Color.Black,
        surface = if (highContrast) Color.White else Color(0xFFFCF9F3),
        onSurface = Color.Black,
        surfaceVariant = if (highContrast) Color(0xFFE8E8E8) else Parchment,
        onSurfaceVariant = if (highContrast) Color.Black else Color(0xFF5C554C),
        outline = if (highContrast) Color.Black else Color(0xFF8A8175),
        outlineVariant = if (highContrast) Color.Black else Color(0xFFD5CCBE),
        error = Error,
        onError = Color.White,
    )

    fun dark(accent: Color = VermilionLight, pureBlack: Boolean = false, highContrast: Boolean = false) = darkColorScheme(
        primary = if (highContrast) Color(0xFFFFB4A2) else accent,
        onPrimary = Color(0xFF3A0B00),
        primaryContainer = if (highContrast) Color(0xFF8A2E16) else accent.copy(alpha = 0.24f),
        onPrimaryContainer = if (highContrast) Color.White else Color(0xFFFFDAD2),
        secondary = Color(0xFFE3C07A),
        onSecondary = Color(0xFF3D2E00),
        secondaryContainer = Color(0xFF54431A),
        onSecondaryContainer = Color(0xFFFFE7B0),
        tertiary = Color(0xFFA9C5A0),
        onTertiary = Color(0xFF12351A),
        background = when {
            pureBlack -> AmoledBlack
            highContrast -> Color.Black
            else -> Night
        },
        onBackground = if (highContrast) Color.White else Color(0xFFEDE6DC),
        surface = when {
            pureBlack -> AmoledBlack
            highContrast -> Color.Black
            else -> NightSurface
        },
        onSurface = if (highContrast) Color.White else Color(0xFFEDE6DC),
        surfaceVariant = if (pureBlack) Color(0xFF111111) else NightCard,
        onSurfaceVariant = if (highContrast) Color.White else Color(0xFFCFC6BA),
        outline = if (highContrast) Color.White else Color(0xFF978F84),
        outlineVariant = if (highContrast) Color.White else Color(0xFF403B35),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )
}

/** Daltonism-aware palette: hues spaced for the most common colour vision types. */
val colorBlindSafePalette = listOf(
    Color(0xFF0072B2), // blue
    Color(0xFFE69F00), // orange
    Color(0xFF009E73), // bluish green
    Color(0xFFCC79A7), // reddish purple
    Color(0xFF56B4E9), // sky blue
    Color(0xFFD55E00), // vermillion
    Color(0xFFF0E442), // yellow
)
