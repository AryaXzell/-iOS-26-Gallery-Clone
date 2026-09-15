package com.aryaxzell.gallery.feature.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import com.aryaxzell.gallery.core.data.EditAdjustments
import com.aryaxzell.gallery.core.data.MediaItem
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class EditTab {
    Adjust,
    Filters,
    Crop
}

@Composable
fun EditScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeItem = uiState.activeEditItem ?: return
    val edits = uiState.currentEdits

    var activeTab by remember { mutableStateOf(EditTab.Adjust) }
    var isHoldingToCompare by remember { mutableStateOf(false) }
    var selectedAdjustTool by remember { mutableStateOf("Exposure") }
    var isRendering by remember { mutableStateOf(false) }
    val drawPaths = remember { mutableStateListOf<Path>() }

    // Color Matrix calculation for live real-time filters
    val colorMatrix = remember(edits, isHoldingToCompare) {
        if (isHoldingToCompare) {
            ColorMatrix()
        } else {
            edits.toColorMatrix()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("edit_screen")
    ) {
        // Center Image Preview with real-time adjustments & markup
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = 180.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHoldingToCompare = true
                            tryAwaitRelease()
                            isHoldingToCompare = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val editImageRequest = remember(activeItem.uri, context) {
                ImageRequest.Builder(context)
                    .data(activeItem.uri)
                    .crossfade(false)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(1440, 1440)
                    .precision(Precision.INEXACT)
                    .allowRgb565(true)
                    .build()
            }

            val aspectRatioValue = remember(edits.cropAspectRatio, isHoldingToCompare) {
                if (isHoldingToCompare) null else {
                    when (edits.cropAspectRatio) {
                        "Square" -> 1f
                        "16:9" -> 16f / 9f
                        "9:16" -> 9f / 16f
                        "4:3" -> 4f / 3f
                        "3:2" -> 3f / 2f
                        else -> null
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .then(
                        if (aspectRatioValue != null) {
                            Modifier
                                .aspectRatio(aspectRatioValue)
                                .clip(RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = editImageRequest,
                    contentDescription = "Edit Preview",
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = if (isHoldingToCompare) 0f else edits.rotationDegrees
                            scaleX = if (!isHoldingToCompare && edits.isFlippedHorizontal) -1f else 1f
                        }
                )
            }

            // "ORIGINAL" floating badge when holding to compare
            if (isHoldingToCompare) {
                GlassSurface(
                    tier = GlassTier.Controls,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "ORIGINAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Top Navigation Bar: Cancel | Revert | Done
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !isRendering
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            if (edits.isModified || drawPaths.isNotEmpty()) {
                TextButton(
                    onClick = {
                        viewModel.revertEdits()
                        drawPaths.clear()
                    },
                    enabled = !isRendering
                ) {
                    Text(
                        text = stringResource(R.string.revert),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF3B30)
                    )
                }
            }

            Button(
                onClick = {
                    if (!isRendering) {
                        coroutineScope.launch {
                            isRendering = true
                            viewModel.saveEdits()
                            delay(650) // High-fidelity iOS render feel
                            isRendering = false
                            onDone()
                        }
                    }
                },
                enabled = !isRendering,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00))
            ) {
                Text(
                    text = stringResource(R.string.done),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Bottom Editor Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // Tab Specific Control Area
            when (activeTab) {
                EditTab.Adjust -> {
                    AdjustToolPanel(
                        edits = edits,
                        selectedTool = selectedAdjustTool,
                        onToolSelected = { tool ->
                            if (tool == "Auto") {
                                viewModel.updateEdits { current ->
                                    current.copy(autoEnhanced = !current.autoEnhanced)
                                }
                            } else {
                                selectedAdjustTool = tool
                            }
                        },
                        onAdjustmentChange = { tool, value ->
                            viewModel.updateEdits { current ->
                                when (tool) {
                                    "Exposure" -> current.copy(exposure = value)
                                    "Brilliance" -> current.copy(brilliance = value)
                                    "Highlights" -> current.copy(highlights = value)
                                    "Shadows" -> current.copy(shadows = value)
                                    "Contrast" -> current.copy(contrast = value)
                                    "Brightness" -> current.copy(brightness = value)
                                    "Saturation" -> current.copy(saturation = value)
                                    "Warmth" -> current.copy(warmth = value)
                                    "Tint" -> current.copy(tint = value)
                                    else -> current
                                }
                            }
                        }
                    )
                }
                EditTab.Filters -> {
                    FiltersToolPanel(
                        currentFilter = edits.filterName,
                        intensity = edits.filterIntensity,
                        onFilterSelected = { filter ->
                            viewModel.updateEdits { it.copy(filterName = filter) }
                        },
                        onIntensityChange = { intensity ->
                            viewModel.updateEdits { it.copy(filterIntensity = intensity) }
                        }
                    )
                }
                EditTab.Crop -> {
                    TransformToolPanel(
                        rotation = edits.rotationDegrees,
                        isFlipped = edits.isFlippedHorizontal,
                        currentAspectRatio = edits.cropAspectRatio,
                        onRotate = {
                            viewModel.updateEdits { it.copy(rotationDegrees = (it.rotationDegrees + 90f) % 360f) }
                        },
                        onFlip = {
                            viewModel.updateEdits { it.copy(isFlippedHorizontal = !it.isFlippedHorizontal) }
                        },
                        onAspectRatioSelected = { ratio ->
                            viewModel.updateEdits { it.copy(cropAspectRatio = ratio) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3-Tab Switcher (Adjust, Filters, Crop)
            GlassSurface(
                tier = GlassTier.Controls,
                shape = CircleShape,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditTabItem(
                        title = stringResource(R.string.edit_tab_adjust),
                        icon = Icons.Default.Tune,
                        isSelected = activeTab == EditTab.Adjust,
                        onClick = { activeTab = EditTab.Adjust }
                    )
                    EditTabItem(
                        title = stringResource(R.string.edit_tab_filters),
                        icon = Icons.Default.Filter,
                        isSelected = activeTab == EditTab.Filters,
                        onClick = { activeTab = EditTab.Filters }
                    )
                    EditTabItem(
                        title = stringResource(R.string.edit_tab_crop),
                        icon = Icons.Default.Crop,
                        isSelected = activeTab == EditTab.Crop,
                        onClick = { activeTab = EditTab.Crop }
                    )
                }
            }
        }

        // iOS-style Rendering Modal Overlay
        AnimatedVisibility(
            visible = isRendering,
            modifier = Modifier.fillMaxSize()
        ) {
            IOSRenderingHUD()
        }
    }
}

@Composable
fun EditTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.6f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

@Composable
fun AdjustToolPanel(
    edits: EditAdjustments,
    selectedTool: String,
    onToolSelected: (String) -> Unit,
    onAdjustmentChange: (String, Float) -> Unit
) {
    val tools = listOf(
        "Auto" to Icons.Default.AutoAwesome,
        "Exposure" to Icons.Default.WbSunny,
        "Brilliance" to Icons.Default.Flare,
        "Highlights" to Icons.Default.Brightness6,
        "Shadows" to Icons.Default.Brightness4,
        "Contrast" to Icons.Default.Contrast,
        "Brightness" to Icons.Default.LightMode,
        "Saturation" to Icons.Default.ColorLens,
        "Warmth" to Icons.Default.WbIncandescent,
        "Tint" to Icons.Default.Palette
    )

    val currentValue = when (selectedTool) {
        "Exposure" -> edits.exposure
        "Brilliance" -> edits.brilliance
        "Highlights" -> edits.highlights
        "Shadows" -> edits.shadows
        "Contrast" -> edits.contrast
        "Brightness" -> edits.brightness
        "Saturation" -> edits.saturation
        "Warmth" -> edits.warmth
        "Tint" -> edits.tint
        else -> 0f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Slider with numeric indicator (hidden when Auto is selected)
        if (selectedTool != "Auto") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(currentValue * 100).toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00),
                    modifier = Modifier.width(36.dp)
                )
                Slider(
                    value = currentValue,
                    onValueChange = { onAdjustmentChange(selectedTool, it) },
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFFCC00),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Auto Enhance Status banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (edits.autoEnhanced) "Auto Enhance: ON" else "Auto Enhance: OFF",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (edits.autoEnhanced) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Tools horizontal selector
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            items(tools) { (name, icon) ->
                val isAutoTool = name == "Auto"
                val isSelected = if (isAutoTool) edits.autoEnhanced else selectedTool == name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onToolSelected(name) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFFFFCC00).copy(alpha = 0.25f)
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) Color(0xFFFFCC00) else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) Color(0xFFFFCC00) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun FiltersToolPanel(
    currentFilter: String,
    intensity: Float,
    onFilterSelected: (String) -> Unit,
    onIntensityChange: (Float) -> Unit
) {
    val filters = listOf("Original", "Vivid", "Dramatic", "Mono", "Silvertone", "Noir", "Warm", "Cool")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Filter intensity slider if a filter is active
        if (currentFilter != "Original") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(intensity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCC00),
                    modifier = Modifier.width(44.dp)
                )
                Slider(
                    value = intensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFFFFCC00),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filters) { filter ->
                val isSelected = currentFilter == filter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onFilterSelected(filter) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFFFCC00).copy(alpha = 0.3f) else Color.DarkGray)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color(0xFFFFCC00) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.take(3).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFFFFCC00) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TransformToolPanel(
    rotation: Float,
    isFlipped: Boolean,
    currentAspectRatio: String,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    onAspectRatioSelected: (String) -> Unit
) {
    val aspectRatios = listOf("Original", "Square", "16:9", "9:16", "4:3", "3:2")
    Column(modifier = Modifier.fillMaxWidth()) {
        // Aspect ratio selector row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
        ) {
            items(aspectRatios) { ratio ->
                val isSelected = currentAspectRatio == ratio
                val containerColor = if (isSelected) Color(0xFFFFCC00).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                val borderColor = if (isSelected) Color(0xFFFFCC00) else Color.Transparent
                val contentColor = if (isSelected) Color(0xFFFFCC00) else Color.White

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor)
                        .clickable { onAspectRatioSelected(ratio) }
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = ratio,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ratio,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }
        }

        // Action buttons: Rotate, Flip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRotate) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = "Rotate 90",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onFlip) {
                Icon(
                    imageVector = Icons.Default.Flip,
                    contentDescription = "Flip",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun IOSRenderingHUD(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { /* intercept clicks during rendering */ },
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            tier = GlassTier.Controls,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .width(190.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IOSActivityIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Saving Changes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rendering photo...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun IOSActivityIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ios_spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        val count = 8
        val strokeWidth = size.minDimension * 0.11f
        val tickLength = size.minDimension * 0.24f
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        for (i in 0 until count) {
            val angleDeg = i * (360f / count)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val alpha = (i + 1).toFloat() / count

            val startX = (center.x + (radius - tickLength) * Math.cos(angleRad)).toFloat()
            val startY = (center.y + (radius - tickLength) * Math.sin(angleRad)).toFloat()
            val endX = (center.x + radius * Math.cos(angleRad)).toFloat()
            val endY = (center.y + radius * Math.sin(angleRad)).toFloat()

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.18f, 1f)),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

