package org.libraryofarts.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Catalogue geometry: nearly square plates and rules, never pill-shaped UI. */
val LibraryShapes = Shapes(
    extraSmall = RoundedCornerShape(1.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)
