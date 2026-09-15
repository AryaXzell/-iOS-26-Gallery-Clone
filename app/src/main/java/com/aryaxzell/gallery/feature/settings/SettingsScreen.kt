package com.aryaxzell.gallery.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.aryaxzell.gallery.BuildConfig
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aryaxzell.gallery.core.designsystem.GlassSurface
import com.aryaxzell.gallery.core.designsystem.GlassTier
import com.aryaxzell.gallery.feature.GalleryUiState
import com.aryaxzell.gallery.feature.GalleryViewModel

@Composable
fun SettingsScreen(
    viewModel: GalleryViewModel,
    uiState: GalleryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassSurface(
                tier = GlassTier.Controls,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Accessibility Section
        Text(
            text = "ACCESSIBILITY & DISPLAY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        GlassSurface(
            tier = GlassTier.Controls,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Reduce Transparency Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Reduce Transparency",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Disables Liquid Glass blur for solid surfaces",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = uiState.settings.reduceTransparency,
                        onCheckedChange = {
                            viewModel.updateSettings(
                                reduceTransparency = it,
                                reduceMotion = uiState.settings.reduceMotion,
                                liquidGlassEnabled = uiState.settings.liquidGlassEnabled
                            )
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF007AFF))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reduce Motion Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.MotionPhotosOff,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Reduce Motion",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Replaces spring physics with instant transitions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = uiState.settings.reduceMotion,
                        onCheckedChange = {
                            viewModel.updateSettings(
                                reduceTransparency = uiState.settings.reduceTransparency,
                                reduceMotion = it,
                                liquidGlassEnabled = uiState.settings.liquidGlassEnabled
                            )
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF007AFF))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Liquid Glass Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.BlurOn,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Liquid Glass",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enables real-time backdrop glass blur",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = uiState.settings.liquidGlassEnabled,
                        onCheckedChange = {
                            viewModel.updateSettings(
                                reduceTransparency = uiState.settings.reduceTransparency,
                                reduceMotion = uiState.settings.reduceMotion,
                                liquidGlassEnabled = it
                            )
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF007AFF))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About section
        Text(
            text = "ABOUT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        GlassSurface(
            tier = GlassTier.Controls,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gallery — iOS 26 Photos Native Clone",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 1.0.0 (Liquid Glass Design System)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
