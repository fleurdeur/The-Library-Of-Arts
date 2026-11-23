package org.libraryofarts.feature.collections

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.libraryofarts.core.designsystem.component.GlassCard
import org.libraryofarts.core.designsystem.component.LibraryPill
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.model.Collection
import org.libraryofarts.core.ui.ArtworkImage
import org.libraryofarts.core.ui.BackButton
import org.libraryofarts.core.ui.ScreenTitle

@Composable
fun CollectionsRoute(
    onBack: () -> Unit,
    onOpenCollection: (String) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importJson(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.share.collect { request ->
            when (request) {
                is ShareRequest.FileShare -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", request.file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = request.mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, request.title)
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
                        Text("Collections", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.Share, contentDescription = "Import a shared collection", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                item("intro") {
                    ScreenTitle(
                        "Your private museum",
                        "Gather works into rooms of your own. Export a collection as a PDF exhibition or a file someone else can import — no account needed.",
                    )
                }
                item("actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ArtButton("New collection", onClick = { showCreate = true }, icon = Icons.Default.Add)
                        ArtButton("Import file", onClick = { importLauncher.launch(arrayOf("*/*")) }, filled = false)
                    }
                }
                if (state.collections.isEmpty()) {
                    item("empty") {
                        EmptyState(
                            title = "No collections yet",
                            message = "Save artworks from any screen, or create a room for an idea you're following.",
                            actionLabel = "Create a collection",
                            onAction = { showCreate = true },
                        )
                    }
                } else {
                    items(state.collections, key = { it.collection.id }) { card ->
                        CollectionCard(
                            card = card,
                            onClick = { onOpenCollection(card.collection.id) },
                            onPdf = { viewModel.exportPdf(card.collection) },
                            onJson = { viewModel.exportJson(card.collection) },
                            onDelete = { viewModel.delete(card.collection) },
                        )
                    }
                }
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New collection") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("A sentence about this room (optional)") },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.create(newName, newDescription)
                    showCreate = false
                    newName = ""
                    newDescription = ""
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CollectionCard(
    card: CollectionCardUi,
    onClick: () -> Unit,
    onPdf: () -> Unit,
    onJson: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 116.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (card.cover != null) {
                    ArtworkImage(card.cover, Modifier.fillMaxSize())
                } else {
                    Text("🖼️", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.collection.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (card.collection.isFavorites) {
                        Spacer(Modifier.width(6.dp))
                        Text("♥", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    "${card.count} ${if (card.count == 1) "work" else "works"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (card.collection.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        card.collection.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LibraryPill("PDF")
                    LibraryPill("Shareable file")
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Collection actions")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Export as PDF exhibition") },
                        onClick = { menu = false; onPdf() },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Export shareable file") },
                        onClick = { menu = false; onJson() },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                    )
                    if (!card.collection.isFavorites) {
                        DropdownMenuItem(
                            text = { Text("Delete collection") },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}
