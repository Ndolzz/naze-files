package com.naze.files.ui.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.naze.files.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    item: FileItem,
    isFavorite: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowInfo: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableStateOf(0) }
    var chromeVisible by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }

    // Swipe-down-to-dismiss: dismissOffsetY tracks the finger 1:1 while
    // dragging, then either springs back to 0 or animates out to hand off
    // to onNavigateBack - see detectSwipeToDismissGesture below. The
    // threshold is a fraction of the actual screen height (from
    // LocalConfiguration, so it adapts to portrait/landscape and any
    // device size) rather than a fixed pixel count.
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val dismissThresholdPx = screenHeightPx * 0.25f
    val dismissOffsetY = remember { Animatable(0f) }
    val dismissProgress = (dismissOffsetY.value / dismissThresholdPx).coerceIn(0f, 1f)

    val imageRequest = remember(item.absolutePath, reloadKey) {
        ImageRequest.Builder(context)
            .data(File(item.absolutePath))
            .crossfade(true)
            .build()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (chromeVisible) {
                TopAppBar(
                    title = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (rotationDegrees != 0) {
                            IconButton(
                                enabled = !isSaving,
                                onClick = {
                                    isSaving = true
                                    scope.launch {
                                        val result = saveRotatedBitmap(File(item.absolutePath), rotationDegrees)
                                        isSaving = false
                                        if (result) {
                                            rotationDegrees = 0
                                            reloadKey++
                                            snackbarHostState.showSnackbar("Rotation saved")
                                        } else {
                                            snackbarHostState.showSnackbar("Could not save rotation")
                                        }
                                    }
                                },
                            ) {
                                Icon(imageVector = Icons.Filled.Save, contentDescription = "Save rotation")
                            }
                        }
                        IconButton(onClick = { rotationDegrees = (rotationDegrees + 90) % 360 }) {
                            Icon(imageVector = Icons.Filled.RotateRight, contentDescription = "Rotate")
                        }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite",
                            )
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = { menuExpanded = false; onShare() },
                            )
                            DropdownMenuItem(
                                text = { Text("Information") },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                onClick = { menuExpanded = false; onShowInfo() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { menuExpanded = false; onDelete() },
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Background fades out as the drag approaches the dismiss
                // threshold, capped so it never goes fully transparent.
                .background(Color.Black.copy(alpha = 1f - dismissProgress * 0.85f))
                .padding(if (chromeVisible) padding else PaddingValues(0.dp))
                .pointerInput(Unit) {
                    detectSwipeToDismissGesture(
                        isZoomed = { scale > 1.02f },
                        onTransform = { zoomChange, panChange ->
                            scale = (scale * zoomChange).coerceIn(1f, 6f)
                            offsetX += panChange.x
                            offsetY += panChange.y
                        },
                        onDismissDrag = { deltaY ->
                            dismissOffsetY.snapTo((dismissOffsetY.value + deltaY).coerceAtLeast(0f))
                        },
                        onDismissRelease = { velocityY ->
                            val flingingDown = velocityY > 1200f
                            if (dismissOffsetY.value > dismissThresholdPx || flingingDown) {
                                dismissOffsetY.animateTo(screenHeightPx, animationSpec = tween(200))
                                onNavigateBack()
                            } else {
                                dismissOffsetY.animateTo(
                                    0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                        },
                        onDismissCancelled = {
                            dismissOffsetY.animateTo(0f, animationSpec = spring())
                        },
                        onTap = { chromeVisible = !chromeVisible },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale * (1f - dismissProgress * 0.15f),
                        scaleY = scale * (1f - dismissProgress * 0.15f),
                        translationX = offsetX,
                        translationY = offsetY + dismissOffsetY.value,
                        rotationZ = rotationDegrees.toFloat(),
                    ),
            )
            if (isSaving) {
                CircularProgressIndicator()
            }
        }
    }
}

private suspend fun saveRotatedBitmap(file: File, degrees: Int): Boolean = withContext(Dispatchers.IO) {
    try {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        val format = if (file.extension.equals("png", ignoreCase = true)) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        FileOutputStream(file).use { out ->
            rotated.compress(format, 92, out)
        }
        if (rotated != original) original.recycle()
        rotated.recycle()
        true
    } catch (e: Exception) {
        false
    }
}
