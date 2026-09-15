package com.aryaxzell.gallery.feature.collections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaxzell.gallery.core.data.MediaItem
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.core.designsystem.PhotoGridItem

@Composable
fun AlbumDetailScreen(
    title: String,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onRestoreSelected: ((List<Long>) -> Unit)? = null,
    onPermanentDeleteSelected: ((List<Long>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isRecentlyDeleted = remember(title) {
        title.equals("Recently Deleted", ignoreCase = true) ||
            title.equals("Baru-baru ini Dihapus", ignoreCase = true) ||
            title.contains("Deleted", ignoreCase = true)
    }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var idsToDeletePermanently by remember { mutableStateOf<List<Long>>(emptyList()) }

    Box(modifier = modifier.fillMaxSize().testTag("album_detail_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassSurface(
                        tier = GlassTier.Controls,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isSelectionMode) {
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                } else {
                                    onBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isSelectionMode) "${selectedIds.size} Selected" else title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${items.size} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (items.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedIds = emptySet()
                            } else {
                                isSelectionMode = true
                            }
                        }
                    ) {
                        Text(
                            text = if (isSelectionMode) "Done" else "Select",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF007AFF)
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRecentlyDeleted) "No deleted items" else "No media in $title",
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
                    items(items, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        PhotoGridItem(
                            thumbnailUri = item.thumbnailUri ?: item.uri,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            isVideo = item.isVideo,
                            durationText = item.formattedDuration(),
                            isFavorite = item.isFavorite,
                            isEdited = item.isEdited,
                            editAdjustments = item.editAdjustments,
                            onTap = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) {
                                        selectedIds - item.id
                                    } else {
                                        selectedIds + item.id
                                    }
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onLongPress = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                }
                                selectedIds = if (isSelected) {
                                    selectedIds - item.id
                                } else {
                                    selectedIds + item.id
                                }
                            }
                        )
                    }
                }
            }
        }

        // Bottom Action Bar for Recently Deleted or Selection Mode
        AnimatedVisibility(
            visible = (isRecentlyDeleted && items.isNotEmpty()) || (isSelectionMode && items.isNotEmpty()),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            GlassSurface(
                tier = GlassTier.Standard,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isRecentlyDeleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Restore Button
                        TextButton(
                            onClick = {
                                val targets = if (selectedIds.isNotEmpty()) selectedIds.toList() else items.map { it.id }
                                onRestoreSelected?.invoke(targets)
                                selectedIds = emptySet()
                                isSelectionMode = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Restore",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedIds.isNotEmpty()) "Restore (${selectedIds.size})" else "Restore All",
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Delete Permanently Button
                        TextButton(
                            onClick = {
                                val targets = if (selectedIds.isNotEmpty()) selectedIds.toList() else items.map { it.id }
                                idsToDeletePermanently = targets
                                showDeleteConfirmDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete Permanently",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedIds.isNotEmpty()) "Delete (${selectedIds.size})" else "Delete All",
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.SemiBold
                            )
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
                        TextButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == items.size) {
                                    emptySet()
                                } else {
                                    items.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedIds.size == items.size) "Deselect All" else "Select All",
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "${selectedIds.size} Selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Confirmation Dialog for Permanent Deletion
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    idsToDeletePermanently = emptyList()
                },
                title = { Text("Delete Permanently?") },
                text = {
                    Text("These ${idsToDeletePermanently.size} item(s) will be deleted permanently. This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onPermanentDeleteSelected?.invoke(idsToDeletePermanently)
                            showDeleteConfirmDialog = false
                            selectedIds = emptySet()
                            isSelectionMode = false
                        }
                    ) {
                        Text("Delete Permanently", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            idsToDeletePermanently = emptyList()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
