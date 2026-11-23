package org.libraryofarts.core.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import org.libraryofarts.core.database.FtsSupport
import org.libraryofarts.core.model.ArtworkFilter
import org.libraryofarts.core.model.SortOrder

internal object ArtworkQueryBuilder {
    fun build(filter: ArtworkFilter, limit: Int? = null, offset: Int = 0): SupportSQLiteQuery {
        val args = mutableListOf<Any?>()
        val sql = StringBuilder()
        val hasQuery = filter.query.isNotBlank()
        val useFts = hasQuery && FtsSupport.available

        if (useFts) {
            sql.append(
                "SELECT a.* FROM artworks a JOIN artwork_fts f ON f.artworkId = a.id " +
                    "WHERE artwork_fts MATCH ?",
            )
            args += ftsExpression(filter.query)
        } else {
            sql.append("SELECT a.* FROM artworks a WHERE 1 = 1")
            if (hasQuery) {
                val like = "%${filter.query.trim()}%"
                sql.append(
                    " AND (a.title LIKE ? OR a.artistName LIKE ? OR a.description LIKE ? OR a.tagsCsv LIKE ? OR a.museumName LIKE ?)",
                )
                repeat(5) { args += like }
            }
        }

        if (filter.genres.isNotEmpty()) {
            sql.append(" AND a.genre IN (${filter.genres.joinToString(",") { "?" }})")
            args.addAll(filter.genres)
        }
        if (filter.periods.isNotEmpty()) {
            sql.append(" AND a.period IN (${filter.periods.joinToString(",") { "?" }})")
            args.addAll(filter.periods)
        }
        if (filter.artists.isNotEmpty()) {
            sql.append(" AND a.artistName IN (${filter.artists.joinToString(",") { "?" }})")
            args.addAll(filter.artists)
        }
        if (filter.museumIds.isNotEmpty()) {
            sql.append(" AND a.museumId IN (${filter.museumIds.joinToString(",") { "?" }})")
            args.addAll(filter.museumIds)
        }
        if (filter.mediums.isNotEmpty()) {
            sql.append(" AND (${filter.mediums.joinToString(" OR ") { "a.medium LIKE ?" }})")
            args.addAll(filter.mediums.map { "%$it%" })
        }
        if (filter.colors.isNotEmpty()) {
            sql.append(" AND (${filter.colors.joinToString(" OR ") { "a.colorFamiliesCsv LIKE ?" }})")
            args.addAll(filter.colors.map { "%$it%" })
        }
        if (filter.moods.isNotEmpty()) {
            sql.append(" AND (${filter.moods.joinToString(" OR ") { "a.moodsCsv LIKE ?" }})")
            args.addAll(filter.moods.map { "%$it%" })
        }
        if (filter.subjects.isNotEmpty()) {
            sql.append(" AND (${filter.subjects.joinToString(" OR ") { "a.subjectsCsv LIKE ?" }})")
            args.addAll(filter.subjects.map { "%$it%" })
        }
        filter.centuryFrom?.let {
            sql.append(" AND a.yearStart >= ?")
            args += (it - 1) * 100
        }
        filter.centuryTo?.let {
            sql.append(" AND a.yearStart <= ?")
            args += it * 100
        }
        if (filter.publicDomainOnly) {
            sql.append(" AND (a.json LIKE '%\"isPublicDomain\":true%' OR a.json LIKE '%\"requiresAttribution\":true%')")
        }

        sql.append(
            when (filter.sort) {
                SortOrder.RELEVANCE -> if (useFts) " ORDER BY rank" else " ORDER BY a.viewCount DESC, a.title ASC"
                SortOrder.NEWEST -> " ORDER BY a.yearStart DESC, a.title ASC"
                SortOrder.OLDEST -> " ORDER BY a.yearStart ASC, a.title ASC"
                SortOrder.MOST_VIEWED -> " ORDER BY a.viewCount DESC, a.title ASC"
                SortOrder.TITLE -> " ORDER BY a.title ASC"
            },
        )
        if (limit != null) {
            sql.append(" LIMIT ? OFFSET ?")
            args += limit
            args += offset
        }
        return SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
    }

    private fun ftsExpression(raw: String): String = raw
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            val safe = token.replace("\"", "")
            "\"$safe\"*"
        }
        .ifBlank { "\"\"" }
}
