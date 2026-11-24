package org.libraryofarts.core.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SectionHeader
import org.libraryofarts.core.designsystem.theme.EditorialFamily
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.FeedItem

@Composable
fun ArtworkImage(
    artwork: Artwork,
    modifier: Modifier = Modifier,
    contentDescription: String? = artwork.image.altText.ifBlank { artwork.title },
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val placeholder = remember(artwork.id) {
        Brush.linearGradient(
            listOf(
                Color(artwork.palette.getOrNull(0)?.argb ?: 0xFF2A2622),
                Color(artwork.palette.getOrNull(1)?.argb ?: 0xFF8A8175),
                Color(artwork.palette.getOrNull(2)?.argb ?: 0xFFE9DFC9),
            ),
        )
    }
    Box(
        modifier = modifier
            .background(placeholder)
            .semantics { this.contentDescription = contentDescription.orEmpty() },
    ) {
        val source = artwork.image.localPath?.let { java.io.File(it) } ?: artwork.image.thumbnailUrl
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(source)
                .memoryCacheKey(source.toString())
                .diskCacheKey(source.toString())
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun ArtworkCard(
    artwork: Artwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    onSave: (() -> Unit)? = null,
    width: Dp = 184.dp,
    showReason: String? = null,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val responsiveWidth = if (width == 184.dp) {
        when {
            screenWidth >= 900 -> 248.dp
            screenWidth >= 600 -> 214.dp
            else -> width
        }
    } else {
        width
    }
    Column(
        modifier = modifier
            .width(responsiveWidth)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.80f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clip(RoundedCornerShape(1.dp)),
        ) {
            ArtworkImage(artwork, Modifier.fillMaxSize())
            if (onSave != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(onClick = onSave),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from favorites" else "Save to favorites",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (showReason != null) {
            Text(
                text = showReason.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = artwork.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = EditorialFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 21.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = artwork.displayArtist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${artwork.museum.shortName} · ${artwork.year}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtworkWideCard(
    artwork: Artwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSave: (() -> Unit)? = null,
    isSaved: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ArtworkImage(
                artwork = artwork,
                modifier = Modifier
                    .size(width = 104.dp, height = 132.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(1.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    artwork.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = EditorialFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${artwork.displayArtist} · ${artwork.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${artwork.museum.shortName} · ${artwork.license.shortLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    artwork.genre?.let { LibraryPill(it) }
                    artwork.displayPeriod?.let { LibraryPill(it) }
                }
            }
            if (onSave != null) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Saved" else "Save",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp)
                        .clickable(onClick = onSave),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
fun ArtworkRow(
    title: String,
    artworks: List<Artwork>,
    onArtworkClick: (Artwork) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    savedIds: Set<String> = emptySet(),
    onSave: ((Artwork) -> Unit)? = null,
) {
    if (artworks.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title, subtitle = subtitle)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(artworks, key = { it.id }) { artwork ->
                ArtworkCard(
                    artwork = artwork,
                    onClick = { onArtworkClick(artwork) },
                    isSaved = artwork.id in savedIds,
                    onSave = onSave?.let { { it(artwork) } },
                )
            }
        }
    }
}

@Composable
fun FeedRow(
    sectionTitle: String,
    items: List<FeedItem>,
    onArtworkClick: (Artwork) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    savedIds: Set<String> = emptySet(),
    onSave: ((Artwork) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = sectionTitle, subtitle = subtitle)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items, key = { it.artwork.id }) { item ->
                ArtworkCard(
                    artwork = item.artwork,
                    onClick = { onArtworkClick(item.artwork) },
                    isSaved = item.artwork.id in savedIds,
                    onSave = onSave?.let { { it(item.artwork) } },
                    showReason = item.explanation,
                )
            }
        }
    }
}

@Composable
fun MiniArtworkChip(artwork: Artwork, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(artwork, Modifier.size(38.dp).clip(RoundedCornerShape(1.dp)))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.padding(end = 10.dp)) {
            Text(artwork.title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(artwork.displayArtist, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun AudioBadge(count: Int, onClick: () -> Unit) {
    if (count <= 0) return
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("$count AUDIO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ArtworkFacts(artwork: Artwork, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        FactRow("Museum", artwork.museum.name)
        FactRow("Location", artwork.location ?: artwork.museum.city)
        artwork.medium?.let { FactRow("Medium", it) }
        artwork.dimensions?.let { FactRow("Dimensions", it) }
        artwork.genre?.let { FactRow("Genre", it) }
        artwork.displayPeriod?.let { FactRow("Period", it) }
        artwork.movement?.let { FactRow("Movement", it) }
        artwork.accessionNumber?.let { FactRow("Accession", it) }
        FactRow("Licence", artwork.license.shortLabel)
        if (artwork.creditLine.isNotBlank()) FactRow("Credit", artwork.creditLine)
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(108.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}
