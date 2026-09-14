package com.example.core.designsystem

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.common.LocalGallerySettings

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

    if (settings.reduceTransparency) {
        // Solid surface fallback for accessibility
        Surface(
            modifier = modifier,
            shape = shape,
            color = tint,
            tonalElevation = when (tier) {
                GlassTier.Controls -> 2.dp
                GlassTier.Standard -> 6.dp
                GlassTier.Utility -> 1.dp
            }
        ) {
            Box(content = content)
        }
    } else {
        // Liquid Glass Surface with spec-exact tint, blur, specular rim highlight, and depth
        Box(
            modifier = modifier
                .shadow(
                    elevation = if (tier == GlassTier.Standard) 12.dp else 4.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.15f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.6f else 0.25f)
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tint.copy(alpha = (baseAlpha + 0.08f).coerceAtMost(0.95f)),
                            tint.copy(alpha = baseAlpha)
                        )
                    )
                )
                .border(
                    width = GlassTokens.edgeBorderWidth,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = highlightAlpha),
                            Color.White.copy(alpha = highlightAlpha * 0.35f)
                        )
                    ),
                    shape = shape
                )
        ) {
            content()
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
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(2.dp))
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .testTag("photo_grid_item_${thumbnailUri.hashCode()}")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbnailUri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
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
