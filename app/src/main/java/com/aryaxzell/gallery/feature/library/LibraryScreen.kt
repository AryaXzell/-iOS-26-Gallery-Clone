package com.aryaxzell.gallery.feature.library

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaxzell.gallery.R
import com.aryaxzell.gallery.core.data.MediaItem
import com.aryaxzell.gallery.core.designsystem.Edge
import com.aryaxzell.gallery.core.designsystem.GlassIconButton
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.core.designsystem.PhotoGridItem
import com.aryaxzell.gallery.core.designsystem.ScrollEdgeGradient
import com.aryaxzell.gallery.core.designsystem.StickyMonthHeader
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel
import com.aryaxzell.gallery.feature.collections.AddToAlbumDialog
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val isScrolled by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 20 }
    }

    // Group media by Month & Year for sticky-like chronological sections
    val groupedByMonth = remember(uiState.filteredMedia) {
        uiState.filteredMedia.groupBy { it.monthYear }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.filteredMedia.isEmpty()) {
            LibrarySkeletonGrid(columns = uiState.gridColumns)
        } else if (uiState.filteredMedia.isEmpty()) {
            // Empty State
            EmptyLibraryState(onLoadDemo = { viewModel.loadDemoGallery() })
        } else {
            // Photo & Video Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(uiState.gridColumns),
                state = gridState,
                contentPadding = PaddingValues(top = 90.dp, bottom = 120.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_grid")
            ) {
                groupedByMonth.forEach { (monthYear, items) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        StickyMonthHeader(label = monthYear)
                    }

                    items(items, key = { it.id }) { item ->
                        val isSelected = uiState.selectedIds.contains(item.id)
                        PhotoGridItem(
                            thumbnailUri = item.uri,
                            isSelected = isSelected,
                            isSelectionMode = uiState.isSelectionMode,
                            onTap = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleItemSelection(item.id)
                                } else {
                                    viewModel.openDetail(item)
                                }
                            },
                            onLongPress = {
                                if (!uiState.isSelectionMode) {
                                    viewModel.toggleSelectionMode()
                                }
                                viewModel.toggleItemSelection(item.id)
                            }
                        )
                    }
                }
            }
        }

        // Scroll Edge Gradient top
        ScrollEdgeGradient(
            visible = isScrolled,
            edge = Edge.Top,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Top Navigation Bar (Standard Glass)
        LibraryTopBar(
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedIds.size,
            gridColumns = uiState.gridColumns,
            onToggleSelection = { viewModel.toggleSelectionMode() },
            onToggleGridDensity = { viewModel.toggleGridDensity() },
            onOpenFilterSheet = { viewModel.setFilterSheetOpen(true) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Selection Mode Bottom Bar
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        ) {
            SelectionActionBar(
                hasSelection = uiState.selectedIds.isNotEmpty(),
                onShare = {
                    val selectedMedia = uiState.filteredMedia.filter { uiState.selectedIds.contains(it.id) }
                    if (selectedMedia.isNotEmpty()) {
                        val shareIntent = if (selectedMedia.size == 1) {
                            val single = selectedMedia.first()
                            Intent(Intent.ACTION_SEND).apply {
                                type = if (single.isVideo) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, single.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        } else {
                            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "image/*"
                                val uris = ArrayList(selectedMedia.map { it.uri })
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                    }
                },
                onFavorite = { viewModel.favoriteSelected() },
                onTrash = { showDeleteConfirmation = true },
                onSelectAll = { viewModel.selectAll() }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmation) {
            val selectedCount = uiState.selectedIds.size
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(if (selectedCount == 1) "Delete Photo?" else "Delete $selectedCount Photos?") },
                text = { Text("This action will move the photo(s) to the Trash album. You can restore them anytime within 30 days.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSelected()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Filter Bottom Sheet
        if (uiState.isFilterSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.setFilterSheetOpen(false) },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                FilterSheetContent(
                    onlyEdited = uiState.filterOnlyEdited,
                    hideScreenshots = uiState.filterHideScreenshots,
                    onOnlyEditedChange = { viewModel.setFilterOnlyEdited(it) },
                    onHideScreenshotsChange = { viewModel.setFilterHideScreenshots(it) },
                    onDismiss = { viewModel.setFilterSheetOpen(false) }
                )
            }
        }
    }
}

@Composable
fun LibraryTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    gridColumns: Int,
    onToggleSelection: () -> Unit,
    onToggleGridDensity: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        tier = GlassTier.Standard,
        shape = CircleShape,
        modifier = modifier
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
            // Left action
            TextButton(
                onClick = onToggleSelection,
                modifier = Modifier.testTag("select_toggle_button")
            ) {
                Text(
                    text = if (isSelectionMode) stringResource(R.string.cancel) else stringResource(R.string.select),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF007AFF)
                )
            }

            // Center Title / Selected count
            if (isSelectionMode) {
                Text(
                    text = stringResource(R.string.selected_count, selectedCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = stringResource(R.string.tab_library),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isSelectionMode) {
                    IconButton(
                        onClick = onToggleGridDensity,
                        modifier = Modifier.size(36.dp).testTag("grid_density_toggle")
                    ) {
                        Icon(
                            imageVector = if (gridColumns == 3) Icons.Default.GridView else Icons.Default.GridOn,
                            contentDescription = "Zoom Density",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenFilterSheet,
                        modifier = Modifier.size(36.dp).testTag("filter_sheet_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionActionBar(
    hasSelection: Boolean,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onTrash: () -> Unit,
    onSelectAll: () -> Unit
) {
    GlassSurface(
        tier = GlassTier.Standard,
        shape = CircleShape,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .height(56.dp)
            .testTag("selection_action_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShare, enabled = hasSelection) {
                Icon(
                    imageVector = Icons.Filled.IosShare,
                    contentDescription = stringResource(R.string.action_share),
                    tint = if (hasSelection) Color(0xFF007AFF) else Color.Gray
                )
            }
            IconButton(onClick = onFavorite, enabled = hasSelection) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.action_favorite),
                    tint = if (hasSelection) Color(0xFF007AFF) else Color.Gray
                )
            }
            IconButton(onClick = onTrash, enabled = hasSelection) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_trash),
                    tint = if (hasSelection) Color(0xFFFF3B30) else Color.Gray
                )
            }
            TextButton(onClick = onSelectAll) {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF007AFF)
                )
            }
        }
    }
}

@Composable
fun FilterSheetContent(
    onlyEdited: Boolean,
    hideScreenshots: Boolean,
    onOnlyEditedChange: (Boolean) -> Unit,
    onHideScreenshotsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Filter Options",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = onlyEdited,
                onCheckedChange = onOnlyEditedChange,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF007AFF))
            )
            Text(
                text = stringResource(R.string.filter_only_edited),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = hideScreenshots,
                onCheckedChange = onHideScreenshotsChange,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF007AFF))
            )
            Text(
                text = stringResource(R.string.filter_hide_screenshots),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text(text = stringResource(R.string.done), color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun EmptyLibraryState(onLoadDemo: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            GlassSurface(
                tier = GlassTier.Standard,
                shape = CircleShape,
                modifier = Modifier.size(88.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.empty_photos_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.empty_photos_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLoadDemo,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                modifier = Modifier.testTag("load_demo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.add_sample_photos))
            }
        }
    }
}

@Composable
fun LibrarySkeletonGrid(
    columns: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
    val highlightColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(top = 90.dp, bottom = 120.dp),
        userScrollEnabled = false,
        modifier = modifier
            .fillMaxSize()
            .testTag("library_skeleton_grid")
    ) {
        items(24) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(1.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(shimmerBrush)
            )
        }
    }
}
