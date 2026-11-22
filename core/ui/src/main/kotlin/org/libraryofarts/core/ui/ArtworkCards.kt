package org.libraryofarts.core.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SectionHeader
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
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artwork.image.localPath?.let { java.io.File(it) } ?: artwork.image.thumbnailUrl)
                .crossfade(true)
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
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(width * 1.22f)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            ArtworkImage(artwork, Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
            )
            if (onSave != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f))
                        .clickable(onClick = onSave),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from favorites" else "Save to favorites",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            ) {
                Text(
                    text = artwork.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artwork.displayArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showReason != null) {
            Text(
                text = showReason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${artwork.museum.shortName} · ${artwork.year}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
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
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(
                artwork = artwork,
                modifier = Modifier
                    .size(width = 112.dp, height = 132.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    artwork.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${artwork.displayArtist} · ${artwork.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    artwork.genre?.let { LibraryPill(it) }
                    artwork.period?.let { LibraryPill(it) }
                }
                if (artwork.license.isPublicDomain) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        artwork.license.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (onSave != null) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Saved" else "Save",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(22.dp)
                        .clickable(onClick = onSave),
                )
            }
        }
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
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(artwork, Modifier.size(40.dp).clip(CircleShape))
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
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("$count audio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
        artwork.period?.let { FactRow("Period", it) }
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
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
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
