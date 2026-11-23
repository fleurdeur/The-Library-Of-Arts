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
 * A two-voice type system. A serif carries titles, captions and the editorial
 * voice; Inter carries interface labels, metadata and body copy. The serif is
 * the platform's own Noto Serif, so it stays crisp and complete for every
 * script without downloading a font at runtime.
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_italic, FontWeight.Normal, style = androidx.compose.ui.text.font.FontStyle.Italic),
    Font(R.font.noto_color_emoji, FontWeight.Normal),
)

val EditorialFamily = FontFamily.Serif

private val baseLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun inter(
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

private fun editorial(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Double = -0.2,
) = TextStyle(
    fontFamily = EditorialFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = baseLineHeight,
)

val LibraryTypography = Typography(
    displayLarge = editorial(56, 58, FontWeight.Normal, -1.4),
    displayMedium = editorial(44, 47, FontWeight.Normal, -1.0),
    displaySmall = editorial(36, 40, FontWeight.Normal, -0.6),
    headlineLarge = editorial(32, 37, FontWeight.Normal, -0.4),
    headlineMedium = editorial(28, 33, FontWeight.Normal, -0.3),
    headlineSmall = editorial(24, 29, FontWeight.Normal, -0.2),
    titleLarge = editorial(21, 27, FontWeight.Normal, -0.1),
    titleMedium = inter(16, 23, FontWeight.SemiBold, 0.0),
    titleSmall = inter(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = inter(17, 27, FontWeight.Normal, 0.1),
    bodyMedium = inter(15, 23, FontWeight.Normal, 0.15),
    bodySmall = inter(13, 20, FontWeight.Normal, 0.2),
    labelLarge = inter(13, 18, FontWeight.SemiBold, 1.0),
    labelMedium = inter(11, 15, FontWeight.Medium, 1.2),
    labelSmall = inter(10, 14, FontWeight.Medium, 1.3),
)
