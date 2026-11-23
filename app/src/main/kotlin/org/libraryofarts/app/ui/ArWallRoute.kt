package org.libraryofarts.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.designsystem.component.ArtButton
import org.libraryofarts.core.designsystem.component.EmptyState
import org.libraryofarts.core.designsystem.component.SoftBackdrop
import org.libraryofarts.core.ui.ArtworkImage
import org.libraryofarts.core.ui.BackButton

@Composable
fun ArWallRoute(artworkId: String, onBack: () -> Unit) {
    val artwork = SeedArtworks.byId[artworkId]
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permission.launch(Manifest.permission.CAMERA) }
    var scale by remember { mutableFloatStateOf(0.62f) }
    var showFrame by remember { mutableStateOf(true) }
    val arAvailability = remember {
        runCatching { ArCoreApk.getInstance().checkAvailability(context) }.getOrNull()
    }

    SoftBackdrop {
        if (artwork == null) {
            EmptyState("Work not found", "This artwork is not installed on the device.", actionLabel = "Back", onAction = onBack)
        } else {
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
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Text("See it on my wall", style = MaterialTheme.typography.titleLarge)
                    }
                }
                item("camera") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(470.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFF2B2420)),
                    ) {
                        if (hasCamera) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).also { view ->
                                        val future = ProcessCameraProvider.getInstance(ctx)
                                        future.addListener({
                                            runCatching {
                                                val provider = future.get()
                                                val preview = Preview.Builder().build().also {
                                                    it.setSurfaceProvider(view.surfaceProvider)
                                                }
                                                provider.unbindAll()
                                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Camera preview", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    "Allow camera access to place the work in your room. The camera is only active here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                                Spacer(Modifier.height(10.dp))
                                ArtButton("Allow camera", onClick = { permission.launch(Manifest.permission.CAMERA) })
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(26.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ArtworkImage(
                                artwork,
                                modifier = Modifier
                                    .fillMaxWidth(scale)
                                    .alpha(0.94f)
                                    .then(
                                        if (showFrame) {
                                            Modifier.border(10.dp, Color(0xFF6B4E2E), RoundedCornerShape(2.dp))
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                        Text(
                            "Scale and frame preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                item("controls") {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Size on the wall", style = MaterialTheme.typography.titleSmall)
                        Slider(value = scale, onValueChange = { scale = it.coerceIn(0.25f, 0.95f) }, valueRange = 0.25f..0.95f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArtButton("Frame on/off", onClick = { showFrame = !showFrame }, filled = false)
                            ArtButton(
                                "Reset",
                                onClick = {
                                    scale = 0.62f
                                    showFrame = true
                                },
                                filled = false,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            when (arAvailability) {
                                ArCoreApk.Availability.SUPPORTED_INSTALLED ->
                                    "ARCore is available. This preview shows scale and proportion; depth placement can be added when the AR pack is installed."
                                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED ->
                                    "ARCore can be installed for depth-aware placement. This camera preview works without it."
                                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD ->
                                    "ARCore needs an update for depth placement. This camera preview works without it."
                                else ->
                                    "This device does not support ARCore. The camera-and-frame preview still shows scale and proportion."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "The artwork never leaves the device. Dimensions come from the museum record; check them against your own wall.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
