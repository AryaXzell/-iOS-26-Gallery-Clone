package com.example.core.data

import android.net.Uri

data class EditAdjustments(
    val autoEnhanced: Boolean = false,
    val exposure: Float = 0f,
    val brilliance: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val contrast: Float = 0f,
    val brightness: Float = 0f,
    val blackPoint: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val isBlackAndWhite: Boolean = false,
    val filterName: String = "Original",
    val filterIntensity: Float = 1.0f,
    val cropAspectRatio: String = "Original",
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontal: Boolean = false
) {
    val isModified: Boolean
        get() = autoEnhanced || exposure != 0f || brilliance != 0f || highlights != 0f ||
                shadows != 0f || contrast != 0f || brightness != 0f || blackPoint != 0f ||
                saturation != 0f || vibrance != 0f || warmth != 0f || tint != 0f ||
                isBlackAndWhite || filterName != "Original" || rotationDegrees != 0f ||
                isFlippedHorizontal || cropAspectRatio != "Original"
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val dateAdded: Long,
    val dateString: String,
    val monthYear: String,
    val dayString: String,
    val duration: Long = 0L,
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isDeleted: Boolean = false,
    val isScreenshot: Boolean = false,
    val isEdited: Boolean = false,
    val editAdjustments: EditAdjustments = EditAdjustments(),
    val location: String = "Unknown Location",
    val cameraModel: String = "iPhone 16 Pro",
    val lensInfo: String = "24 mm f/1.78 ISO 50",
    val resolution: String = "48 MP • 8064 × 6048",
    val fileSize: String = "4.2 MB",
    val categoryTag: String = "Nature",
    val tripName: String? = null,
    val personOrPetName: String? = null
)
