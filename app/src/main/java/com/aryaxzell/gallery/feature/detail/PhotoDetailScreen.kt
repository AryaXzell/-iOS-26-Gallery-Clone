package com.aryaxzell.gallery.feature.detail

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.aryaxzell.gallery.R
import com.aryaxzell.gallery.core.data.MediaItem
import com.aryaxzell.gallery.core.designsystem.GlassIconButton
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.core.util.MediaMetadataExtractor
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel
import com.aryaxzell.gallery.feature.collections.AddToAlbumDialog
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.ui.graphics.ColorFilter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    onBack: () -> Unit,
    onEdit: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawItems = remember(uiState.activeDetailList, uiState.activeAlbumDetail, uiState.filteredMedia, uiState.allMedia) {
        uiState.activeDetailList
            ?: uiState.activeAlbumDetail?.second
            ?: uiState.filteredMedia.ifEmpty { uiState.allMedia }
    }
    val currentItem = uiState.activeDetailItem ?: return
    val items = remember(rawItems, currentItem) {
        if (rawItems.any { it.id == currentItem.id }) rawItems else listOf(currentItem) + rawItems
    }
    val initialPage = remember(items, currentItem.id) {
        items.indexOfFirst { it.id == currentItem.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { items.size })

    LaunchedEffect(pagerState.currentPage, items) {
        val visible = items.getOrNull(pagerState.currentPage)
        if (visible != null && visible.id != currentItem.id) {
            viewModel.setActiveDetailItem(visible)
        }
    }

    var showToolbars by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showTrashConfirm by remember { mutableStateOf(false) }
    var showDeletePermanentlyConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Tilt sensor for Spatial Scene Parallax
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(uiState.isSpatialMode) {
        if (uiState.isSpatialMode) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null) {
                        tiltX = (-event.values[0] * 5f).coerceIn(-30f, 30f)
                        tiltY = (event.values[1] * 5f).coerceIn(-30f, 30f)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager?.unregisterListener(listener) }
        } else {
            tiltX = 0f
            tiltY = 0f
            onDispose { }
        }
    }

    var isZoomed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("photo_detail_screen")
    ) {
        // Fullscreen Horizontal Pager
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items.getOrNull(page) ?: return@HorizontalPager

            if (item.isVideo) {
                // Video Player with iOS 26 overlay controls
                IOSVideoPlayer(
                    item = item,
                    isActivePage = (pagerState.currentPage == page),
                    showControls = showToolbars,
                    onToggleControls = { showToolbars = !showToolbars }
                )
            } else {
                // Photo Viewer with zoom, pan, and spatial tilt
                var scale by remember(page) { mutableFloatStateOf(1f) }
                var offset by remember(page) { mutableStateOf(Offset.Zero) }

                LaunchedEffect(scale, pagerState.currentPage) {
                    isZoomed = (pagerState.currentPage == page) && (scale > 1.05f)
                }

                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 4f)
                    if (scale > 1.05f) {
                        val maxOffsetLimit = 400f * (scale - 1f)
                        offset = Offset(
                            x = (offset.x + panChange.x).coerceIn(-maxOffsetLimit, maxOffsetLimit),
                            y = (offset.y + panChange.y).coerceIn(-maxOffsetLimit, maxOffsetLimit)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showToolbars = !showToolbars },
                                onDoubleTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val detailImageRequest = remember(item.uri, context) {
                        ImageRequest.Builder(context)
                            .data(item.uri)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .size(1920, 1920)
                            .precision(Precision.INEXACT)
                            .allowRgb565(true)
                            .build()
                    }

                    val detailRatioValue = remember(item.editAdjustments.cropAspectRatio) {
                        when (item.editAdjustments.cropAspectRatio) {
                            "Square" -> 1f
                            "16:9" -> 16f / 9f
                            "9:16" -> 9f / 16f
                            "4:3" -> 4f / 3f
                            "3:2" -> 3f / 2f
                            else -> null
                        }
                    }

                    AsyncImage(
                        model = detailImageRequest,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (detailRatioValue != null) Modifier.aspectRatio(detailRatioValue).clip(RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .transformable(state = transformState)
                            .graphicsLayer {
                                scaleX = scale * if (item.isEdited && item.editAdjustments.isFlippedHorizontal) -1f else 1f
                                scaleY = scale
                                rotationZ = if (item.isEdited) item.editAdjustments.rotationDegrees else 0f
                                translationX = offset.x + tiltX
                                translationY = offset.y + tiltY
                            },
                        colorFilter = if (item.isEdited) ColorFilter.colorMatrix(item.editAdjustments.toColorMatrix()) else null
                    )
                }
            }
        }

        // Top Toolbar (Utility Glass, auto-hide)
        AnimatedVisibility(
            visible = showToolbars,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val activeItem = items.getOrNull(pagerState.currentPage) ?: currentItem
            GlassSurface(
                tier = GlassTier.Utility,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = activeItem.dayString,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = activeItem.dateString,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Spatial 3D Hexagon icon
                        IconButton(onClick = { viewModel.toggleSpatialMode() }) {
                            Icon(
                                imageVector = Icons.Default.Hexagon,
                                contentDescription = "Spatial Scene",
                                tint = if (uiState.isSpatialMode) Color(0xFF007AFF) else Color.White
                            )
                        }

                        if (!activeItem.isDeleted) {
                            IconButton(onClick = {
                                viewModel.toggleHidden(activeItem.id, activeItem.isHidden)
                            }) {
                                Icon(
                                    imageVector = if (activeItem.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (activeItem.isHidden) "Unhide" else "Hide",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Toolbar: [Share -> Favorite -> Info -> Edit -> Trash] (Utility glass)
        AnimatedVisibility(
            visible = showToolbars,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            val activeItem = items.getOrNull(pagerState.currentPage) ?: currentItem

            GlassSurface(
                tier = GlassTier.Utility,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("photo_detail_bottom_bar")
            ) {
                if (activeItem.isDeleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.restoreMedia(activeItem.id)
                                onBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Restore Photo",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                showDeletePermanentlyConfirm = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete Permanently",
                                tint = Color(0xFFFF3B30)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Permanently", color = Color(0xFFFF3B30))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Share
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (activeItem.isVideo) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, activeItem.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                        }) {
                            Icon(
                                imageVector = Icons.Filled.IosShare,
                                contentDescription = stringResource(R.string.action_share),
                                tint = Color.White
                            )
                        }

                        // 2. Favorite
                        IconButton(onClick = {
                            viewModel.toggleFavorite(activeItem.id, activeItem.isFavorite)
                        }) {
                            Icon(
                                imageVector = if (activeItem.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.action_favorite),
                                tint = if (activeItem.isFavorite) Color(0xFFFF3B30) else Color.White
                            )
                        }

                        // 3. Add to Album
                        IconButton(onClick = { showAddToAlbumDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add to Album",
                                tint = Color.White
                            )
                        }

                        // 4. Info (i)
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.action_info),
                                tint = Color.White
                            )
                        }

                        // 5. Edit (three horizontal sliders / tune)
                        IconButton(onClick = { onEdit(activeItem) }) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = Color.White
                            )
                        }

                        // 6. Trash
                        IconButton(onClick = {
                            showTrashConfirm = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_trash),
                                tint = Color(0xFFFF3B30)
                            )
                        }
                    }
                }
            }
        }

        // Info Bottom Sheet
        if (showInfoSheet) {
            val activeItem = items.getOrNull(pagerState.currentPage) ?: currentItem
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PhotoInfoSheet(item = activeItem)
            }
        }

        // Add to Album Dialog
        if (showAddToAlbumDialog) {
            val activeItem = items.getOrNull(pagerState.currentPage) ?: currentItem
            AddToAlbumDialog(
                customAlbums = uiState.customAlbums,
                onDismiss = { showAddToAlbumDialog = false },
                onSelectAlbum = { album ->
                    viewModel.addMediaToAlbum(album.id, activeItem.id)
                    showAddToAlbumDialog = false
                },
                onCreateNewAlbum = { title ->
                    viewModel.createAlbumWithMedia(title, listOf(activeItem.id))
                    showAddToAlbumDialog = false
                }
            )
        }

        // Trash confirmation dialog
        if (showTrashConfirm) {
            val activeItem = items.getOrNull(pagerState.currentPage) ?: currentItem
            AlertDialog(
                onDismissRequest = { showTrashConfirm = false },
                title = { Text("Delete Photo?") },
                text = { Text("This action will move the photo to the Trash album. You can restore them anytime within 30 days.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMedia(activeItem.id)
                            showTrashConfirm = false
                            onBack()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTrashConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Permanent deletion confirmation dialog
        if (showDeletePermanentlyConfirm) {
            val activeItem = items.getOrNull(pagerState.currentPage) ?: currentItem
            AlertDialog(
                onDismissRequest = { showDeletePermanentlyConfirm = false },
                title = { Text("Delete Permanently?") },
                text = { Text("This action cannot be undone. This item will be permanently deleted from this device.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePermanently(activeItem.id)
                            showDeletePermanentlyConfirm = false
                            onBack()
                        }
                    ) {
                        Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePermanentlyConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
}

@Composable
fun IOSVideoPlayer(
    item: MediaItem,
    isActivePage: Boolean = true,
    showControls: Boolean,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember(item.id) { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var currentPosition by remember(item.id) { mutableFloatStateOf(0f) }
    var totalDuration by remember(item.id, item.durationMs) {
        mutableFloatStateOf(if (item.durationMs > 0) item.durationMs.toFloat() else 15000f)
    }

    val exoPlayer = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(isActivePage) {
        if (!isActivePage) {
            isPlaying = false
            exoPlayer.pause()
        }
    }

    DisposableEffect(item.id) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Real-time ExoPlayer tracking loop (only runs when playing and not scrubbing)
    LaunchedEffect(isPlaying, isScrubbing, item.id) {
        if (isPlaying && !isScrubbing) {
            while (true) {
                currentPosition = exoPlayer.currentPosition.toFloat()
                val duration = exoPlayer.duration
                if (duration > 0) {
                    totalDuration = duration.toFloat()
                }
                delay(200)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleControls() })
            }
    ) {
        // Media3 ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Custom iOS Controls
                }
            },
            update = { view ->
                view.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top-Left Mute/Unmute 40dp round glass button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 70.dp, start = 16.dp)
                ) {
                    GlassIconButton(
                        icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                        tier = GlassTier.Controls
                    )
                }

                // Center Play/Pause 56dp round glass button
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSurface(
                        tier = GlassTier.Controls,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isPlaying = !isPlaying
                                if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Fine-grain Scrub Bar near bottom (thin white progress line) with duration status bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = formatDuration(currentPosition.toLong()),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val maxWidthPx = constraints.maxWidth.toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val fraction = (offset.x / maxWidthPx).coerceIn(0f, 1f)
                                            val newPos = fraction * totalDuration
                                            currentPosition = newPos
                                            exoPlayer.seekTo(newPos.toLong())
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { isScrubbing = true },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newPos = (currentPosition + (dragAmount.x / maxWidthPx) * totalDuration)
                                                    .coerceIn(0f, totalDuration)
                                                currentPosition = newPos
                                            },
                                            onDragEnd = {
                                                exoPlayer.seekTo(currentPosition.toLong())
                                                isScrubbing = false
                                            },
                                            onDragCancel = {
                                                isScrubbing = false
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val fraction = if (totalDuration > 0f) currentPosition / totalDuration else 0f
                                // Track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                )
                                // Active Fill
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                        .height(4.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Text(
                            text = "-" + formatDuration((totalDuration - currentPosition).toLong().coerceAtLeast(0L)),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoInfoSheet(item: MediaItem) {
    val context = LocalContext.current
    val meta = remember(item.id, item.uri) {
        MediaMetadataExtractor.extract(context, item)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = item.dateString,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Camera, lens & specs card
        GlassSurface(
            tier = GlassTier.Controls,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.PlayArrow else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = meta.cameraModel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${meta.lensInfo} • ${meta.resolution} • ${meta.fileSize}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                if (meta.extraSpecs.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = meta.extraSpecs,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Location card
        GlassSurface(
            tier = GlassTier.Controls,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = meta.location,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (meta.location.contains(",")) "Recorded via GPS metadata" else "No GPS tag embedded in media",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
