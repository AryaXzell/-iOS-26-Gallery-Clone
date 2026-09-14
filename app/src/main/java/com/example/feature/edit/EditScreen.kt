package com.example.feature.edit

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
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
import coil.request.ImageRequest
import com.example.R
import com.example.core.data.EditAdjustments
import com.example.core.data.MediaItem
import com.example.core.designsystem.GlassSurface
import com.example.core.designsystem.GlassTier
import com.example.feature.GalleryUiState
import com.example.feature.GalleryViewModel

enum class EditTab {
    Adjust,
    Filters,
    Crop,
    Markup
}

data class DrawPath(val path: Path, val color: Color, val strokeWidth: Float)

@Composable
fun EditScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeItem = uiState.activeEditItem ?: return
    val edits = uiState.currentEdits

    var activeTab by remember { mutableStateOf(EditTab.Adjust) }
    var isHoldingToCompare by remember { mutableStateOf(false) }
    var selectedAdjustTool by remember { mutableStateOf("Exposure") }

    // Markup drawing paths
    val drawPaths = remember { mutableStateListOf<DrawPath>() }
    var currentDrawPath by remember { mutableStateOf<Path?>(null) }
    var activeBrushColor by remember { mutableStateOf(Color.Yellow) }

    // Color Matrix calculation for live real-time filters
    val colorMatrix = remember(edits, isHoldingToCompare) {
        if (isHoldingToCompare) {
            ColorMatrix()
        } else {
            val cm = ColorMatrix()
            // Brightness / Exposure
            val b = edits.exposure * 60f + edits.brightness * 40f
            cm[0, 4] = b
            cm[1, 4] = b
            cm[2, 4] = b

            // Saturation / Filter effect
            var sat = 1f + edits.saturation + edits.vibrance * 0.5f
            if (edits.filterName == "Mono" || edits.isBlackAndWhite) sat = 0f
            if (edits.filterName == "Vivid") sat += 0.4f
            if (edits.filterName == "Dramatic") sat += 0.2f
            cm.setToSaturation(sat.coerceAtLeast(0f))

            // Contrast
            val c = 1f + edits.contrast * 0.5f
            val scaleMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, 0f,
                    0f, c, 0f, 0f, 0f,
                    0f, 0f, c, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.timesAssign(scaleMatrix)
            cm
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
            AsyncImage(
                model = ImageRequest.Builder(context).data(activeItem.uri).crossfade(false).build(),
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

            // Markup Drawing Canvas Layer
            if (activeTab == EditTab.Markup) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeBrushColor) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                    currentDrawPath = newPath
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentDrawPath?.lineTo(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    currentDrawPath?.let {
                                        drawPaths.add(DrawPath(it, activeBrushColor, 8f))
                                    }
                                    currentDrawPath = null
                                }
                            )
                        }
                ) {
                    drawPaths.forEach { dp ->
                        drawPath(dp.path, dp.color, style = Stroke(dp.strokeWidth, cap = StrokeCap.Round))
                    }
                    currentDrawPath?.let { p ->
                        drawPath(p, activeBrushColor, style = Stroke(8f, cap = StrokeCap.Round))
                    }
                }
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
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            if (edits.isModified) {
                TextButton(onClick = { viewModel.revertEdits() }) {
                    Text(
                        text = stringResource(R.string.revert),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF3B30)
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.saveEdits()
                    onDone()
                },
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
                        onToolSelected = { selectedAdjustTool = it },
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
                    CropToolPanel(
                        rotation = edits.rotationDegrees,
                        isFlipped = edits.isFlippedHorizontal,
                        onRotate = {
                            viewModel.updateEdits { it.copy(rotationDegrees = (it.rotationDegrees + 90f) % 360f) }
                        },
                        onFlip = {
                            viewModel.updateEdits { it.copy(isFlippedHorizontal = !it.isFlippedHorizontal) }
                        }
                    )
                }
                EditTab.Markup -> {
                    MarkupToolPanel(
                        activeColor = activeBrushColor,
                        onColorSelected = { activeBrushColor = it },
                        onUndo = { if (drawPaths.isNotEmpty()) drawPaths.removeAt(drawPaths.size - 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4-Tab Switcher (Adjust, Filters, Crop, Markup)
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
                    EditTabItem(
                        title = stringResource(R.string.edit_tab_markup),
                        icon = Icons.Default.Brush,
                        isSelected = activeTab == EditTab.Markup,
                        onClick = { activeTab = EditTab.Markup }
                    )
                }
            }
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
        "Exposure" to Icons.Default.WbSunny,
        "Brilliance" to Icons.Default.AutoAwesome,
        "Highlights" to Icons.Default.Brightness6,
        "Contrast" to Icons.Default.Contrast,
        "Brightness" to Icons.Default.WbSunny,
        "Saturation" to Icons.Default.InvertColors,
        "Warmth" to Icons.Default.Opacity
    )

    val currentValue = when (selectedTool) {
        "Exposure" -> edits.exposure
        "Brilliance" -> edits.brilliance
        "Highlights" -> edits.highlights
        "Contrast" -> edits.contrast
        "Brightness" -> edits.brightness
        "Saturation" -> edits.saturation
        "Warmth" -> edits.warmth
        else -> 0f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Slider with numeric indicator
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

        // Tools horizontal selector
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            items(tools) { (name, icon) ->
                val isSelected = selectedTool == name
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
    val filters = listOf("Original", "Vivid", "Dramatic", "Mono", "Silvertone", "Noir")

    Column(modifier = Modifier.fillMaxWidth()) {
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
fun CropToolPanel(
    rotation: Float,
    isFlipped: Boolean,
    onRotate: () -> Unit,
    onFlip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
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

@Composable
fun MarkupToolPanel(
    activeColor: Color,
    onColorSelected: (Color) -> Unit,
    onUndo: () -> Unit
) {
    val colors = listOf(Color.Yellow, Color.Red, Color.White, Color(0xFF007AFF), Color.Green)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEach { col ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(
                            width = if (activeColor == col) 2.5.dp else 1.dp,
                            color = if (activeColor == col) Color.White else Color.Black.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(col) }
                )
            }
        }
        TextButton(onClick = onUndo) {
            Text("Undo", color = Color(0xFFFFCC00))
        }
    }
}
