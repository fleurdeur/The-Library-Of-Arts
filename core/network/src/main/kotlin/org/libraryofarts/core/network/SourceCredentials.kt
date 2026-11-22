package org.libraryofarts.core.network

/**
 * Keys are supplied at build time from the developer's untracked
 * local.properties. The repository never contains a key, and sources whose key
 * is absent simply stay disabled while the rest of the library works offline.
 */
data class SourceCredentials(
    val rijksmuseumKey: String = "",
    val smithsonianKey: String = "",
    val europeanaKey: String = "",
    val harvardKey: String = "",
    val parisMuseesKey: String = "",
    val gettyKey: String = "",
    val ngaKey: String = "",
) {
    val configuredSources: Int
        get() = listOf(
            rijksmuseumKey, smithsonianKey, europeanaKey,
            harvardKey, parisMuseesKey, gettyKey, ngaKey,
        ).count { it.isNotBlank() }
}
