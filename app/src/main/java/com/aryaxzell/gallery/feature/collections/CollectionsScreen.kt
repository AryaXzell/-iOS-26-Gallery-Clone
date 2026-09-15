package com.aryaxzell.gallery.feature.collections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.aryaxzell.gallery.R
import com.aryaxzell.gallery.core.data.MediaItem
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel

@Composable
fun CollectionsScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collapsedSections = remember { mutableStateMapOf<String, Boolean>() }
    var showNewAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumTitle by remember { mutableStateOf("") }

    // Pre-calculate sections
    val recentDays = remember(uiState.allMedia) {
        uiState.allMedia.groupBy { it.dayString }.entries.toList()
    }

    val peopleAndPets = remember(uiState.allMedia) {
        uiState.allMedia.filter { it.personOrPetName != null }.distinctBy { it.personOrPetName }
    }

    val trips = remember(uiState.allMedia) {
        uiState.allMedia.filter { it.tripName != null }.groupBy { it.tripName ?: "" }.entries.toList()
    }

    val favoriteItems = remember(uiState.allMedia) {
        uiState.allMedia.filter { it.isFavorite }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("collections_screen"),
        contentPadding = PaddingValues(top = 90.dp, bottom = 130.dp)
    ) {
        // 1. Recent Days
        item {
            CollectionSectionHeader(
                title = stringResource(R.string.section_recent_days),
                isCollapsed = collapsedSections["recent_days"] == true,
                onToggleCollapse = {
                    collapsedSections["recent_days"] = !(collapsedSections["recent_days"] ?: false)
                }
            )
            AnimatedVisibility(visible = collapsedSections["recent_days"] != true) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    items(recentDays) { (day, items) ->
                        RecentDayCard(
                            dayTitle = day,
                            coverItem = items.first(),
                            photoCount = items.size,
                            onClick = { viewModel.openAlbumDetail(day, items) }
                        )
                    }
                }
            }
        }

        // 2. People & Pets
        item {
            CollectionSectionHeader(
                title = stringResource(R.string.section_people_pets),
                isCollapsed = collapsedSections["people_pets"] == true,
                onToggleCollapse = {
                    collapsedSections["people_pets"] = !(collapsedSections["people_pets"] ?: false)
                }
            )
            AnimatedVisibility(visible = collapsedSections["people_pets"] != true) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    items(peopleAndPets) { item ->
                        PeoplePetCard(
                            name = item.personOrPetName ?: "Friend",
                            coverItem = item,
                            onClick = {
                                val personItems = uiState.allMedia.filter { it.personOrPetName == item.personOrPetName }
                                viewModel.openAlbumDetail(item.personOrPetName ?: "Person", personItems)
                            }
                        )
                    }
                }
            }
        }

        // 3. Pinned Collections (Favorites default)
        item {
            CollectionSectionHeader(
                title = stringResource(R.string.section_pinned),
                isCollapsed = collapsedSections["pinned"] == true,
                onToggleCollapse = {
                    collapsedSections["pinned"] = !(collapsedSections["pinned"] ?: false)
                }
            )
            AnimatedVisibility(visible = collapsedSections["pinned"] != true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
                    val highlightTitle = "Highlights $currentYear"
                    val highlights = remember(uiState.allMedia) {
                        val favs = uiState.allMedia.filter { it.isFavorite }
                        if (favs.isNotEmpty()) favs.take(12) else uiState.allMedia.take(12)
                    }

                    PinnedCard(
                        title = stringResource(R.string.util_favorites),
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFFF3B30),
                        count = favoriteItems.size,
                        coverUri = favoriteItems.firstOrNull()?.uri,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openAlbumDetail("Favorites", favoriteItems) }
                    )
                    PinnedCard(
                        title = highlightTitle,
                        icon = Icons.Default.AutoAwesome,
                        iconTint = Color(0xFF007AFF),
                        count = highlights.size,
                        coverUri = highlights.firstOrNull()?.uri,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openAlbumDetail(highlightTitle, highlights) }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 4. Memories
        item {
            CollectionSectionHeader(
                title = stringResource(R.string.section_memories),
                isCollapsed = collapsedSections["memories"] == true,
                onToggleCollapse = {
                    collapsedSections["memories"] = !(collapsedSections["memories"] ?: false)
                }
            )
            AnimatedVisibility(visible = collapsedSections["memories"] != true) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    val memoryList = listOf(
                        "Golden Summer" to uiState.allMedia.filter { it.categoryTag == "Nature" || it.isFavorite },
                        "Wanderlust Moments" to uiState.allMedia.filter { it.tripName != null },
                        "Life with Pets" to uiState.allMedia.filter { it.categoryTag == "Pets" }
                    )
                    items(memoryList) { (title, photos) ->
                        if (photos.isNotEmpty()) {
                            MemoryCard(
                                title = title,
                                coverItem = photos.first(),
                                onClick = { viewModel.openMemory(photos) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Trips
        item {
            CollectionSectionHeader(
                title = stringResource(R.string.section_trips),
                isCollapsed = collapsedSections["trips"] == true,
                onToggleCollapse = {
                    collapsedSections["trips"] = !(collapsedSections["trips"] ?: false)
                }
            )
            AnimatedVisibility(visible = collapsedSections["trips"] != true) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    items(trips) { (trip, photos) ->
                        TripCard(
                            tripName = trip,
                            coverItem = photos.first(),
                            count = photos.size,
                            onClick = { viewModel.openAlbumDetail(trip, photos) }
                        )
                    }
                }
            }
        }

        // 6. Albums
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.section_albums),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showNewAlbumDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Album",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Album", color = Color(0xFF007AFF))
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(uiState.customAlbums) { album ->
                    val albumMedia = uiState.allMedia.filter { it.customAlbumIds.contains(album.id) }
                    AlbumItemCard(
                        albumName = album.name,
                        coverUri = albumMedia.firstOrNull()?.uri?.toString() ?: album.coverUri ?: "",
                        photoCount = albumMedia.size,
                        onClick = { viewModel.openAlbumDetail(album.name, albumMedia) }
                    )
                }
            }
        }

        // 7. Media Types
        item {
            CollectionSectionHeader(
                title = stringResource(R.string.section_media_types),
                isCollapsed = collapsedSections["media_types"] == true,
                onToggleCollapse = {
                    collapsedSections["media_types"] = !(collapsedSections["media_types"] ?: false)
                }
            )
            AnimatedVisibility(visible = collapsedSections["media_types"] != true) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    val videos = uiState.allMedia.filter { it.isVideo }
                    val selfies = uiState.allMedia.filter { it.categoryTag == "Selfies" }
                    val portraits = uiState.allMedia.filter { it.categoryTag == "Portraits" }
                    val screenshots = uiState.allMedia.filter { it.isScreenshot }
                    val panoramas = uiState.allMedia.filter { it.categoryTag == "Panoramas" }

                    MediaCategoryRow(
                        title = stringResource(R.string.media_videos),
                        icon = Icons.Default.Videocam,
                        count = videos.size,
                        onClick = { viewModel.openAlbumDetail("Videos", videos) }
                    )
                    MediaCategoryRow(
                        title = stringResource(R.string.media_selfies),
                        icon = Icons.Default.CameraAlt,
                        count = selfies.size,
                        onClick = { viewModel.openAlbumDetail("Selfies", selfies) }
                    )
                    MediaCategoryRow(
                        title = stringResource(R.string.media_portraits),
                        icon = Icons.Default.Person,
                        count = portraits.size,
                        onClick = { viewModel.openAlbumDetail("Portraits", portraits) }
                    )
                    MediaCategoryRow(
                        title = stringResource(R.string.media_screenshots),
                        icon = Icons.Default.PhotoCamera,
                        count = screenshots.size,
                        onClick = { viewModel.openAlbumDetail("Screenshots", screenshots) }
                    )
                    MediaCategoryRow(
                        title = stringResource(R.string.media_panoramas),
                        icon = Icons.Default.Landscape,
                        count = panoramas.size,
                        onClick = { viewModel.openAlbumDetail("Panoramas", panoramas) }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 8. Utilities
        item {
            CollectionSectionHeader(
                title = stringResource(R.string.section_utilities),
                isCollapsed = collapsedSections["utilities"] == true,
                onToggleCollapse = {
                    collapsedSections["utilities"] = !(collapsedSections["utilities"] ?: false)
                }
            )
            AnimatedVisibility(visible = collapsedSections["utilities"] != true) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    MediaCategoryRow(
                        title = stringResource(R.string.util_favorites),
                        icon = Icons.Default.Favorite,
                        count = favoriteItems.size,
                        onClick = { viewModel.openAlbumDetail("Favorites", favoriteItems) }
                    )
                    MediaCategoryRow(
                        title = stringResource(R.string.util_hidden),
                        icon = Icons.Outlined.Lock,
                        count = uiState.hiddenMedia.size,
                        onClick = { viewModel.openAlbumDetail("Hidden", uiState.hiddenMedia) }
                    )
                    MediaCategoryRow(
                        title = stringResource(R.string.util_recently_deleted),
                        icon = Icons.Outlined.Delete,
                        count = uiState.deletedMedia.size,
                        onClick = { viewModel.openAlbumDetail("Recently Deleted", uiState.deletedMedia) }
                    )
                    MediaCategoryRow(
                        title = stringResource(R.string.util_imports),
                        icon = Icons.Default.FileDownload,
                        count = uiState.allMedia.size,
                        onClick = { viewModel.openAlbumDetail("Imports", uiState.allMedia) }
                    )
                }
            }
        }
    }

    // Top Navigation Glass Bar
    Box(modifier = Modifier.fillMaxSize()) {
        GlassSurface(
            tier = GlassTier.Standard,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tab_collections),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF007AFF)
                    )
                }
            }
        }
    }

    // Create Album Dialog
    if (showNewAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showNewAlbumDialog = false },
            title = { Text("New Album", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newAlbumTitle,
                    onValueChange = { newAlbumTitle = it },
                    placeholder = { Text("Album Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAlbumTitle.isNotBlank()) {
                            viewModel.createAlbum(newAlbumTitle)
                            newAlbumTitle = ""
                            showNewAlbumDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAlbumDialog = false }) {
                    Text("Cancel", color = Color(0xFF007AFF))
                }
            }
        )
    }
}

@Composable
fun CollectionSectionHeader(
    title: String,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isCollapsed) -90f else 0f, label = "arrow_rot")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleCollapse() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = "Toggle Section",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation)
        )
    }
}

@Composable
fun RecentDayCard(
    dayTitle: String,
    coverItem: MediaItem,
    photoCount: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    GlassSurface(
        tier = GlassTier.Controls,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .width(220.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val dayImageRequest = remember(coverItem.uri, context) {
                ImageRequest.Builder(context)
                    .data(coverItem.uri)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(400, 300)
                    .precision(Precision.INEXACT)
                    .build()
            }
            AsyncImage(
                model = dayImageRequest,
                contentDescription = dayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = dayTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$photoCount photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PeoplePetCard(
    name: String,
    coverItem: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
        ) {
            val avatarImageRequest = remember(coverItem.uri, context) {
                ImageRequest.Builder(context)
                    .data(coverItem.uri)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(200, 200)
                    .precision(Precision.INEXACT)
                    .build()
            }
            AsyncImage(
                model = avatarImageRequest,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PinnedCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    count: Int,
    coverUri: Any?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    GlassSurface(
        tier = GlassTier.Controls,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverUri != null) {
                val mediaTypeImageRequest = remember(coverUri, context) {
                    ImageRequest.Builder(context)
                        .data(coverUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(300, 200)
                        .precision(Precision.INEXACT)
                        .build()
                }
                AsyncImage(
                    model = mediaTypeImageRequest,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$count items",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryCard(
    title: String,
    coverItem: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    GlassSurface(
        tier = GlassTier.Controls,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .width(260.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val memoryImageRequest = remember(coverItem.uri, context) {
                ImageRequest.Builder(context)
                    .data(coverItem.uri)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(520, 360)
                    .precision(Precision.INEXACT)
                    .build()
            }
            AsyncImage(
                model = memoryImageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Memory",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = "MEMORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TripCard(
    tripName: String,
    coverItem: MediaItem,
    count: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    GlassSurface(
        tier = GlassTier.Controls,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val tripImageRequest = remember(coverItem.uri, context) {
                ImageRequest.Builder(context)
                    .data(coverItem.uri)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(360, 280)
                    .precision(Precision.INEXACT)
                    .build()
            }
            AsyncImage(
                model = tripImageRequest,
                contentDescription = tripName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = tripName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count photos",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AlbumItemCard(
    albumName: String,
    coverUri: String,
    photoCount: Int = 0,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            if (coverUri.isNotEmpty()) {
                val albumImageRequest = remember(coverUri, context) {
                    ImageRequest.Builder(context)
                        .data(coverUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(280, 280)
                        .precision(Precision.INEXACT)
                        .build()
                }
                AsyncImage(
                    model = albumImageRequest,
                    contentDescription = albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = albumName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "$photoCount items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun MediaCategoryRow(
    title: String,
    icon: ImageVector,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
