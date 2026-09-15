package com.aryaxzell.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aryaxzell.gallery.core.common.LocalGallerySettings
import com.aryaxzell.gallery.core.designsystem.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.aryaxzell.gallery.core.designsystem.GlassBlobBottomBar
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel
import com.aryaxzell.gallery.feature.collections.AlbumDetailScreen
import com.aryaxzell.gallery.feature.collections.CollectionsScreen
import com.aryaxzell.gallery.feature.detail.PhotoDetailScreen
import com.aryaxzell.gallery.feature.edit.EditScreen
import com.aryaxzell.gallery.feature.library.LibraryScreen
import com.aryaxzell.gallery.feature.memories.MemoriesPlayerScreen
import com.aryaxzell.gallery.feature.search.SearchScreen
import com.aryaxzell.gallery.feature.settings.SettingsScreen
import com.aryaxzell.gallery.ui.theme.GalleryTheme

enum class RootScreen {
    MainTabs,
    Search,
    Detail,
    Edit,
    MemoryPlayer,
    AlbumDetail,
    Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalleryTheme {
                GalleryApp()
            }
        }
    }
}

@Composable
fun GalleryApp(viewModel: GalleryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableIntStateOf(0) } // 0 = Library, 1 = Collections
    var currentRootScreen by remember { mutableStateOf(RootScreen.MainTabs) }

    // Request permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadMedia()
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    // React to detail & edit item changes from ViewModel
    LaunchedEffect(uiState.activeDetailItem) {
        if (uiState.activeDetailItem != null && currentRootScreen != RootScreen.Edit) {
            currentRootScreen = RootScreen.Detail
        } else if (uiState.activeDetailItem == null && currentRootScreen == RootScreen.Detail) {
            currentRootScreen = RootScreen.MainTabs
        }
    }

    LaunchedEffect(uiState.activeEditItem) {
        if (uiState.activeEditItem != null) {
            currentRootScreen = RootScreen.Edit
        }
    }

    LaunchedEffect(uiState.activeMemoryPhotos) {
        if (uiState.activeMemoryPhotos != null) {
            currentRootScreen = RootScreen.MemoryPlayer
        }
    }

    LaunchedEffect(uiState.activeAlbumDetail) {
        if (uiState.activeAlbumDetail != null) {
            currentRootScreen = RootScreen.AlbumDetail
        }
    }

    // System Back Handling
    BackHandler(enabled = currentRootScreen != RootScreen.MainTabs || uiState.isSelectionMode) {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else {
            when (currentRootScreen) {
                RootScreen.Search -> currentRootScreen = RootScreen.MainTabs
                RootScreen.Detail -> viewModel.closeDetail()
                RootScreen.Edit -> viewModel.closeEdit()
                RootScreen.MemoryPlayer -> viewModel.closeMemory()
                RootScreen.AlbumDetail -> viewModel.closeAlbumDetail()
                RootScreen.Settings -> currentRootScreen = RootScreen.MainTabs
                RootScreen.MainTabs -> { /* Exit app */ }
            }
        }
    }

    val hazeState = remember { HazeState() }
    val settings = uiState.settings

    CompositionLocalProvider(
        LocalGallerySettings provides settings,
        LocalHazeState provides hazeState
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (settings.liquidGlassEnabled && !settings.reduceTransparency) {
                        Modifier.haze(hazeState)
                    } else {
                        Modifier
                    }
                )
        ) {
            // Screen Switcher
            when (currentRootScreen) {
                RootScreen.MainTabs -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (currentTab == 0) {
                            LibraryScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                        } else {
                            CollectionsScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onOpenSettings = { currentRootScreen = RootScreen.Settings }
                            )
                        }

                        // iOS 26 Floating Blob Bar (Bottom-left switcher + Bottom-right search)
                        AnimatedVisibility(
                            visible = !uiState.isSelectionMode,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            GlassBlobBottomBar(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it },
                                onSearchClick = { currentRootScreen = RootScreen.Search }
                            )
                        }
                    }
                }
                RootScreen.Search -> {
                    SearchScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onCloseSearch = { currentRootScreen = RootScreen.MainTabs }
                    )
                }
                RootScreen.Detail -> {
                    PhotoDetailScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onBack = { viewModel.closeDetail() },
                        onEdit = { item -> viewModel.openEdit(item) }
                    )
                }
                RootScreen.Edit -> {
                    EditScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onDone = { currentRootScreen = RootScreen.Detail },
                        onCancel = { viewModel.closeEdit() }
                    )
                }
                RootScreen.MemoryPlayer -> {
                    MemoriesPlayerScreen(
                        photos = uiState.activeMemoryPhotos ?: emptyList(),
                        onClose = {
                            viewModel.closeMemory()
                            currentRootScreen = RootScreen.MainTabs
                        }
                    )
                }
                RootScreen.AlbumDetail -> {
                    val albumPair = uiState.activeAlbumDetail
                    AlbumDetailScreen(
                        title = albumPair?.first ?: "Album",
                        items = albumPair?.second ?: emptyList(),
                        onBack = {
                            viewModel.closeAlbumDetail()
                            currentRootScreen = RootScreen.MainTabs
                        },
                        onItemClick = { item -> viewModel.openDetail(item) }
                    )
                }
                RootScreen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onBack = { currentRootScreen = RootScreen.MainTabs }
                    )
                }
            }
        }
    }
}
