package com.aryaxzell.gallery.core.designsystem

import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.aryaxzell.gallery.core.common.LocalGallerySettings
import com.aryaxzell.gallery.core.data.EditAdjustments

@Composable
fun GlassSurface(
    tier: GlassTier,
    shape: Shape = RoundedCornerShape(tokenForTier(tier)),
    tint: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val settings = LocalGallerySettings.current

    val baseAlpha = GlassTokens.tintAlpha(tier, isDark)
    val highlightAlpha = GlassTokens.edgeHighlightAlpha(isDark)
    val blurRadius = GlassTokens.blurRadius(tier)

    val shadowElevation = when (tier) {
        GlassTier.Standard -> 4.dp
        GlassTier.Controls -> 2.dp
        GlassTier.Utility -> 0.dp
    }
    val ambientShadowColor = remember(isDark) { Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f) }
    val spotShadowColor = remember(isDark) { Color.Black.copy(alpha = if (isDark) 0.40f else 0.12f) }

    if (settings.reduceTransparency) {
        // Solid surface fallback for accessibility
        Surface(
            modifier = modifier,
            shape = shape,
            color = tint,
            tonalElevation = shadowElevation
        ) {
            val contentColor = if (isDark) Color.White else Color(0xFF1C1C1E)
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Box(content = content)
            }
        }
    } else {
        val effectiveAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            baseAlpha
        } else {
            (baseAlpha + 0.05f).coerceAtMost(0.95f)
        }

        val backgroundGradient = remember(tint, effectiveAlpha, settings.liquidGlassEnabled, isDark) {
            if (settings.liquidGlassEnabled) {
                Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = (effectiveAlpha + 0.06f).coerceAtMost(0.98f)),
                        tint.copy(alpha = effectiveAlpha),
                        tint.copy(alpha = (effectiveAlpha - 0.04f).coerceAtLeast(0.65f))
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = (effectiveAlpha + 0.04f).coerceAtMost(0.98f)),
                        tint.copy(alpha = effectiveAlpha)
                    )
                )
            }
        }

        val borderBrush = remember(highlightAlpha, isDark, settings.liquidGlassEnabled) {
            Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = if (settings.liquidGlassEnabled) 0.28f else 0.16f),
                        Color.White.copy(alpha = if (settings.liquidGlassEnabled) 0.08f else 0.04f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = if (settings.liquidGlassEnabled) 0.85f else 0.65f),
                        Color.Black.copy(alpha = if (settings.liquidGlassEnabled) 0.08f else 0.05f)
                    )
                }
            )
        }

        // Liquid Glass Surface with spec-exact tint, blur, specular rim highlight, and depth
        Box(
            modifier = modifier
                .then(
                    if (shadowElevation > 0.dp) {
                        Modifier.shadow(
                            elevation = if (settings.liquidGlassEnabled) shadowElevation + 2.dp else shadowElevation,
                            shape = shape,
                            ambientColor = ambientShadowColor,
                            spotColor = spotShadowColor
                        )
                    } else Modifier
                )
                .clip(shape)
                .border(
                    width = GlassTokens.edgeBorderWidth,
                    brush = borderBrush,
                    shape = shape
                )
        ) {
            // Background Liquid Glass layer with real blur render effect
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(if (settings.liquidGlassEnabled) blurRadius else blurRadius / 2)
                        } else {
                            Modifier
                        }
                    )
                    .background(backgroundGradient)
            )

            // Specular gleam when liquid glass is enabled (curved glass refraction highlight)
            if (settings.liquidGlassEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Transparent
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(300f, 300f)
                            )
                        )
                )
            }

            val contentColor = if (isDark) Color.White else Color(0xFF1C1C1E)
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}

@Composable
fun GlassFloatingTabBar(
    tabs: List<TabItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = LocalGallerySettings.current
    val isDark = isSystemInDarkTheme()

    GlassSurface(
        tier = GlassTier.Standard,
        shape = CircleShape,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.id == selectedTab
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.08f)
                    } else Color.Transparent,
                    animationSpec = GlassTokens.animationSpec(settings.reduceMotion),
                    label = "tab_pill_bg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(pillBg)
                        .clickable { onTabSelected(tab.id) }
                        .testTag("tab_item_${tab.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) {
                                Color(0xFF007AFF)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tier: GlassTier = GlassTier.Controls
) {
    val size = when (tier) {
        GlassTier.Controls -> 44.dp
        GlassTier.Standard -> 52.dp
        GlassTier.Utility -> 40.dp
    }

    GlassSurface(
        tier = tier,
        shape = CircleShape,
        modifier = Modifier.size(size)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .testTag("glass_icon_${contentDescription.replace(" ", "_").lowercase()}")
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun StickyMonthHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun PhotoGridItem(
    thumbnailUri: Uri,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    isEdited: Boolean = false,
    editAdjustments: EditAdjustments = EditAdjustments(),
    isVideo: Boolean = false,
    durationText: String? = null,
    isFavorite: Boolean = false
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val imageRequest = remember(thumbnailUri, isVideo, context) {
        ImageRequest.Builder(context)
            .data(thumbnailUri)
            .apply {
                if (isVideo) {
                    videoFrameMillis(1000)
                }
            }
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .size(280, 280)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .build()
    }

    val colorFilter = remember(isEdited, editAdjustments) {
        if (isEdited && editAdjustments.isModified) {
            ColorFilter.colorMatrix(editAdjustments.toColorMatrix())
        } else null
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .testTag("photo_grid_item_${thumbnailUri.hashCode()}")
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = if (isVideo) "Video thumbnail" else "Photo thumbnail",
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isEdited && (editAdjustments.rotationDegrees != 0f || editAdjustments.isFlippedHorizontal)) {
                        Modifier.graphicsLayer {
                            rotationZ = editAdjustments.rotationDegrees
                            scaleX = if (editAdjustments.isFlippedHorizontal) -1f else 1f
                        }
                    } else Modifier
                )
        )

        // Video gradient scrim and indicator badge
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                if (!durationText.isNullOrBlank()) {
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // Favorite heart icon indicator
        if (isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = Color(0xFFFF2D55),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Dim overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
            )
        }

        // Selection badge in bottom-right corner [Section 5.5 confirmed default]
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF007AFF) else Color.Black.copy(alpha = 0.40f)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScrollEdgeGradient(
    visible: Boolean,
    edge: Edge,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (edge == Edge.Top) {
                            listOf(baseColor.copy(alpha = 0.75f), Color.Transparent)
                        } else {
                            listOf(Color.Transparent, baseColor.copy(alpha = 0.75f))
                        }
                    )
                )
        )
    }
}
