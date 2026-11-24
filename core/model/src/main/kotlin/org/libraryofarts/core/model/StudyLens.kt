package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

/**
 * Interpretive technical-study lenses. These are colour-matrix readings over
 * the visible plate, not museum imaging data; the labels always say so in the
 * UI. NONE leaves the plate untouched.
 */
@Serializable
enum class StudyLens(val label: String, val note: String) {
    NONE("Visible light", "The painting as the eye receives it."),
    INFRARED("Infrared", "A false-colour reading that can suggest underdrawing and changes beneath the surface."),
    X_RAY("X-ray", "An inverted, high-contrast view of density and structure."),
    UNDERDRAWING("Underdrawing", "A flattened blue reading that pulls line and shadow forward."),
    BEFORE("Before restoration", "A warmer, faded treatment, like an older varnish and older colour balance."),
    AFTER("After restoration", "A cleaner, more saturated treatment, like a freshly cleaned surface."),
}
