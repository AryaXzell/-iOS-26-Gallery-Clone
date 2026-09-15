package com.aryaxzell.gallery.feature.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aryaxzell.gallery.R
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.core.designsystem.PhotoGridItem
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel

@Composable
fun SearchScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val chips = listOf("All", "Photos", "Videos", "Screenshots", "Favorites")

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        // Top Search Bar (Controls Glass capsule)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassSurface(
                tier = GlassTier.Controls,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(Color(0xFF007AFF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth().testTag("search_input_field")
                        )
                    }
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onCloseSearch,
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF007AFF)
                )
            }
        }

        // Filter chips row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(chips) { chip ->
                val selected = uiState.searchFilterChip == chip
                GlassSurface(
                    tier = GlassTier.Controls,
                    shape = CircleShape,
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) Color(0xFF007AFF) else Color.Transparent)
                            .clickable { viewModel.setSearchFilterChip(chip) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Content Area: If Query is empty, show curated categories & recent searches.
        // If Query is not empty, show Live Filter Grid!
        if (uiState.searchQuery.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Recent searches
                if (uiState.recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { viewModel.clearSearchHistory() }) {
                            Text("Clear", color = Color(0xFF007AFF), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    uiState.recentSearches.take(4).forEach { recent ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSearchQuery(recent.query) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = recent.query,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeRecentSearch(recent.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val categories = remember(uiState.allMedia, uiState.customAlbums) {
                    val list = mutableListOf<Triple<String, Any?, Int>>()
                    val videos = uiState.allMedia.filter { it.isVideo }
                    if (videos.isNotEmpty()) {
                        list.add(Triple("Videos", videos.first().uri, videos.size))
                    }
                    val favorites = uiState.allMedia.filter { it.isFavorite }
                    if (favorites.isNotEmpty()) {
                        list.add(Triple("Favorites", favorites.first().uri, favorites.size))
                    }
                    val edited = uiState.allMedia.filter { it.isEdited }
                    if (edited.isNotEmpty()) {
                        list.add(Triple("Edited", edited.first().uri, edited.size))
                    }
                    val screenshots = uiState.allMedia.filter { it.isScreenshot }
                    if (screenshots.isNotEmpty()) {
                        list.add(Triple("Screenshots", screenshots.first().uri, screenshots.size))
                    }
                    uiState.customAlbums.forEach { album ->
                        val albumMedia = uiState.allMedia.filter { it.customAlbumIds.contains(album.id) }
                        if (albumMedia.isNotEmpty()) {
                            list.add(Triple(album.name, albumMedia.first().uri, albumMedia.size))
                        }
                    }
                    if (list.isEmpty() && uiState.allMedia.isNotEmpty()) {
                        list.add(Triple("Photos", uiState.allMedia.first().uri, uiState.allMedia.size))
                    }
                    list
                }

                if (categories.isNotEmpty()) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    categories.chunked(2).forEach { rowPair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPair.forEach { (cat, cover, count) ->
                                SearchCategoryCard(
                                    name = cat,
                                    coverData = cover,
                                    count = count,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (chips.contains(cat)) {
                                            viewModel.setSearchFilterChip(cat)
                                        } else {
                                            viewModel.setSearchQuery(cat)
                                        }
                                    }
                                )
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            // Live Filtered Grid
            if (uiState.filteredMedia.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No results for \"${uiState.searchQuery}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredMedia, key = { it.id }) { item ->
                        PhotoGridItem(
                            thumbnailUri = item.thumbnailUri ?: item.uri,
                            isSelected = false,
                            isSelectionMode = false,
                            isVideo = item.isVideo,
                            durationText = item.formattedDuration(),
                            isFavorite = item.isFavorite,
                            isEdited = item.isEdited,
                            editAdjustments = item.editAdjustments,
                            onTap = { viewModel.openDetail(item, uiState.filteredMedia) },
                            onLongPress = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchCategoryCard(
    name: String,
    coverData: Any?,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    GlassSurface(
        tier = GlassTier.Controls,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverData != null) {
                val categoryImageRequest = remember(coverData, context) {
                    ImageRequest.Builder(context)
                        .data(coverData)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(400, 300)
                        .build()
                }
                AsyncImage(
                    model = categoryImageRequest,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (count > 0) {
                    Text(
                        text = "$count items",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
