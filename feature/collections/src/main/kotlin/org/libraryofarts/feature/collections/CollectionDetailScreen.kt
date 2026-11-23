package org.libraryofarts.feature.collections

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.EmptyState
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.ui.ArtworkImage
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.ScreenTitle

@Composable
fun CollectionDetailRoute(
    onBack: () -> Unit,
    onOpenArtwork: (String) -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.share.collect { request ->
            when (request) {
                is ShareRequest.FileShare -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", request.file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = request.mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share “${request.title}”"))
                }
                is ShareRequest.Message -> snackbar.showSnackbar(request.text)
            }
        }
    }

    SoftBackdrop {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
            ) {
                item("top") {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BackButton(onBack)
                        Spacer(Modifier.width(10.dp))
                        Text("Collection", style = MaterialTheme.typography.titleLarge)
                    }
                }
                item("title") {
                    ScreenTitle(
                        state.collection?.name ?: "Collection",
                        state.collection?.description?.ifBlank { null }
                            ?: "${state.artworks.size} works, held together by your attention.",
                    )
                }
                item("actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ArtButton("PDF exhibition", onClick = viewModel::exportPdf, icon = Icons.Default.PictureAsPdf)
                        ArtButton("Share file", onClick = viewModel::exportJson, icon = Icons.Default.Share, filled = false)
                    }
                }
                if (state.artworks.isEmpty()) {
                    item("empty") {
                        EmptyState(
                            title = "This room is empty",
                            message = "Open an artwork and tap the bookmark to place it here.",
                        )
                    }
                } else {
                    state.artworks.chunked(2).forEachIndexed { index, pair ->
                        item("row-$index") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                pair.forEach { artwork ->
                                    CollectionArtworkTile(
                                        artwork = artwork,
                                        modifier = Modifier.weight(1f),
                                        onOpen = { onOpenArtwork(artwork.id) },
                                        onRemove = { viewModel.remove(artwork) },
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun CollectionArtworkTile(
    artwork: Artwork,
    modifier: Modifier,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(1.dp))
                .clickable(onClick = onOpen),
        ) {
            ArtworkImage(artwork, Modifier.fillMaxSize())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    artwork.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    artwork.displayArtist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Remove ${artwork.title} from this collection",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
