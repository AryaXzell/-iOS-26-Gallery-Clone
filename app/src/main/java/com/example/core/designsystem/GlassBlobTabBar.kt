package com.example.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.common.LocalGallerySettings

/**
 * iOS 26 Floating Bottom Bar Architecture:
 * - Left-bottom: Single "glass blob" switching Library <-> Collections with expand animation
 * - Right-bottom: Separate circular glass button for Search
 */
@Composable
fun GlassBlobBottomBar(
    currentTab: Int, // 0 = Library, 1 = Collections
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = LocalGallerySettings.current
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Blob: Library <-> Collections switcher pill
        GlassSurface(
            tier = GlassTier.Standard,
            shape = CircleShape,
            modifier = Modifier
                .height(52.dp)
                .animateContentSize(animationSpec = GlassTokens.animationSpec(settings.reduceMotion))
                .testTag("glass_blob_switcher")
        ) {
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Library button
                val isLibrary = currentTab == 0
                val libPillBg by animateColorAsState(
                    targetValue = if (isLibrary) {
                        if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.08f)
                    } else Color.Transparent,
                    animationSpec = GlassTokens.animationSpec(settings.reduceMotion),
                    label = "lib_pill_bg"
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(libPillBg)
                        .clickable { onTabSelected(0) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = stringResource(R.string.tab_library),
                            tint = if (isLibrary) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(19.dp)
                        )
                        if (isLibrary) {
                            Text(
                                text = stringResource(R.string.tab_library),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }

                // Collections button
                val isCollections = currentTab == 1
                val colPillBg by animateColorAsState(
                    targetValue = if (isCollections) {
                        if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.08f)
                    } else Color.Transparent,
                    animationSpec = GlassTokens.animationSpec(settings.reduceMotion),
                    label = "col_pill_bg"
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(colPillBg)
                        .clickable { onTabSelected(1) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = stringResource(R.string.tab_collections),
                            tint = if (isCollections) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(19.dp)
                        )
                        if (isCollections) {
                            Text(
                                text = stringResource(R.string.tab_collections),
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

        // Right circular glass button: Search
        GlassSurface(
            tier = GlassTier.Controls,
            shape = CircleShape,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable { onSearchClick() }
                .testTag("glass_search_button")
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.tab_search),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
