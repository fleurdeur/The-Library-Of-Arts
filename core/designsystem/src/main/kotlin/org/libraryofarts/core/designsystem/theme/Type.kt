package org.libraryofarts.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import org.libraryofarts.core.designsystem.R

/**
 * Inter everywhere, with a bundled open emoji font as the final fallback.
 * No downloadable fonts are used, so typography is identical offline.
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_italic, FontWeight.Normal, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(R.font.noto_color_emoji, FontWeight.Normal),
)

private val baseLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = baseLineHeight,
)

val LibraryTypography = Typography(
    displayLarge = style(57, 62, FontWeight.SemiBold, -1.2),
    displayMedium = style(45, 50, FontWeight.SemiBold, -0.8),
    displaySmall = style(36, 42, FontWeight.SemiBold, -0.5),
    headlineLarge = style(32, 38, FontWeight.SemiBold, -0.3),
    headlineMedium = style(28, 34, FontWeight.SemiBold, -0.2),
    headlineSmall = style(24, 30, FontWeight.SemiBold, 0.0),
    titleLarge = style(22, 28, FontWeight.SemiBold, 0.1),
    titleMedium = style(17, 24, FontWeight.Medium, 0.15),
    titleSmall = style(15, 20, FontWeight.Medium, 0.2),
    bodyLarge = style(17, 26, FontWeight.Normal, 0.15),
    bodyMedium = style(15, 23, FontWeight.Normal, 0.2),
    bodySmall = style(13, 19, FontWeight.Normal, 0.25),
    labelLarge = style(15, 20, FontWeight.Medium, 0.1),
    labelMedium = style(12, 16, FontWeight.Medium, 0.5),
    labelSmall = style(11, 15, FontWeight.Medium, 0.5),
)
