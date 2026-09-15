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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aryaxzell.gallery.core.common.LocalGallerySettings
import com.aryaxzell.gallery.core.designsystem.GlassBlobBottomBar
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel
import com.aryaxzell.gallery.feature.collections.AlbumDetailScreen
import com.aryaxzell.gallery.feature.collections.CollectionsScreen
import com.aryaxzell.gallery.feature.detail.PhotoDetailScreen
import com.aryaxzell.gallery.feature.edit.EditScreen
import com.aryaxzell.gallery.feature.library.LibraryScreen
import com.aryaxzell.gallery.feature.library.LibrarySkeletonGrid
import com.aryaxzell.gallery.feature.memories.MemoriesPlayerScreen
import com.aryaxzell.gallery.feature.search.SearchScreen
import com.aryaxzell.gallery.feature.settings.SettingsScreen
import com.aryaxzell.gallery.ui.theme.GalleryTheme
import kotlinx.coroutines.delay

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
    var previousRootScreen by remember { mutableStateOf(RootScreen.MainTabs) }
    var isAppColdStarting by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(800) // Brief initial launch reveal while background queries warm up
        isAppColdStarting = false
    }

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
            if (currentRootScreen != RootScreen.Detail) {
                previousRootScreen = currentRootScreen
            }
            currentRootScreen = RootScreen.Detail
        } else if (uiState.activeDetailItem == null && currentRootScreen == RootScreen.Detail) {
            currentRootScreen = if (uiState.activeAlbumDetail != null) RootScreen.AlbumDetail else previousRootScreen
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
        if (uiState.activeAlbumDetail != null && currentRootScreen != RootScreen.Detail) {
            currentRootScreen = RootScreen.AlbumDetail
        }
    }

    // Synchronized System Back Handling
    val backEnabled = uiState.isSelectionMode ||
            currentRootScreen != RootScreen.MainTabs ||
            currentTab != 0

    BackHandler(enabled = backEnabled) {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else {
            when (currentRootScreen) {
                RootScreen.Search -> currentRootScreen = RootScreen.MainTabs
                RootScreen.Detail -> {
                    viewModel.closeDetail()
                    currentRootScreen = if (uiState.activeAlbumDetail != null) RootScreen.AlbumDetail else previousRootScreen
                }
                RootScreen.Edit -> {
                    viewModel.closeEdit()
                    currentRootScreen = RootScreen.Detail
                }
                RootScreen.MemoryPlayer -> {
                    viewModel.closeMemory()
                    currentRootScreen = RootScreen.MainTabs
                }
                RootScreen.AlbumDetail -> {
                    viewModel.closeAlbumDetail()
                    currentRootScreen = RootScreen.MainTabs
                    currentTab = 1
                }
                RootScreen.Settings -> {
                    currentRootScreen = RootScreen.MainTabs
                    currentTab = 1
                }
                RootScreen.MainTabs -> {
                    if (currentTab != 0) {
                        currentTab = 0
                    }
                }
            }
        }
    }

    val settings = uiState.settings

    CompositionLocalProvider(
        LocalGallerySettings provides settings
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Smooth Screen Switcher
                AnimatedContent(
                    targetState = currentRootScreen,
                    transitionSpec = {
                        if (targetState == RootScreen.Detail || targetState == RootScreen.Edit || targetState == RootScreen.MemoryPlayer) {
                            (fadeIn(androidx.compose.animation.core.tween(240)) + slideInVertically(androidx.compose.animation.core.tween(260)) { it / 8 })
                                .togetherWith(fadeOut(androidx.compose.animation.core.tween(200)))
                        } else {
                            fadeIn(androidx.compose.animation.core.tween(220))
                                .togetherWith(fadeOut(androidx.compose.animation.core.tween(200)) + slideOutVertically(androidx.compose.animation.core.tween(240)) { it / 8 })
                        }
                    },
                    label = "RootScreenTransition",
                    modifier = Modifier.fillMaxSize()
                ) { targetScreen ->
                    when (targetScreen) {
                        RootScreen.MainTabs -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = {
                                        fadeIn(androidx.compose.animation.core.tween(200))
                                            .togetherWith(fadeOut(androidx.compose.animation.core.tween(160)))
                                    },
                                    label = "MainTabsTransition",
                                    modifier = Modifier.fillMaxSize()
                                ) { tab ->
                                    if (tab == 0) {
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
                                onBack = {
                                    viewModel.closeDetail()
                                    currentRootScreen = if (uiState.activeAlbumDetail != null) RootScreen.AlbumDetail else previousRootScreen
                                },
                                onEdit = { item -> viewModel.openEdit(item) }
                            )
                        }
                        RootScreen.Edit -> {
                            EditScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onDone = { currentRootScreen = RootScreen.Detail },
                                onCancel = {
                                    viewModel.closeEdit()
                                    currentRootScreen = RootScreen.Detail
                                }
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
                                    currentTab = 1
                                },
                                onItemClick = { item ->
                                    viewModel.openDetail(item, albumPair?.second)
                                },
                                onRestoreSelected = { ids ->
                                    viewModel.restoreSelected(ids)
                                },
                                onPermanentDeleteSelected = { ids ->
                                    viewModel.deletePermanentlySelected(ids)
                                }
                            )
                        }
                        RootScreen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                uiState = uiState,
                                onBack = {
                                    currentRootScreen = RootScreen.MainTabs
                                    currentTab = 1
                                }
                            )
                        }
                    }
                }

                // Custom iOS Launch Splash & Shimmer Skeleton (Never blank black)
                AnimatedVisibility(
                    visible = isAppColdStarting && uiState.allMedia.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(350)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    IOSAppLaunchScreen()
                }
            }
        }
    }
}

@Composable
fun IOSAppLaunchScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Shimmering skeleton grid behind the launch badge
        LibrarySkeletonGrid(columns = 3, modifier = Modifier.fillMaxSize().alpha(0.35f))

        // Center Frosted Launch Plate
        GlassSurface(
            tier = GlassTier.Controls,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .padding(32.dp)
                .width(220.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Photos Iris Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF2D55),
                                    Color(0xFFFF9500),
                                    Color(0xFFFFCC00),
                                    Color(0xFF34C759),
                                    Color(0xFF007AFF),
                                    Color(0xFF5856D6),
                                    Color(0xFFAF52DE),
                                    Color(0xFFFF2D55)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Photos",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading Library...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

