package com.example.core.common

import androidx.compose.runtime.compositionLocalOf

data class GallerySettings(
    val reduceTransparency: Boolean = false,
    val reduceMotion: Boolean = false
)

val LocalGallerySettings = compositionLocalOf { GallerySettings() }
