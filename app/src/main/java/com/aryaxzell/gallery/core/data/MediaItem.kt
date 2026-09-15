package com.aryaxzell.gallery.core.data

import android.net.Uri
import androidx.compose.ui.graphics.ColorMatrix
import org.json.JSONObject

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

    fun toColorMatrix(): ColorMatrix {
        val matrix = ColorMatrix()
        if (!isModified) return matrix

        var baseSat = (saturation + 100f) / 100f
        var baseContrast = (contrast + 100f) / 100f
        var expFactor = 1f + (exposure / 100f)

        if (autoEnhanced) {
            expFactor *= 1.08f
            baseContrast *= 1.1f
            baseSat *= 1.12f
        }

        when (filterName) {
            "Vivid" -> {
                baseSat *= (1f + 0.35f * filterIntensity)
                baseContrast *= (1f + 0.15f * filterIntensity)
            }
            "Dramatic" -> {
                baseContrast *= (1f + 0.35f * filterIntensity)
                baseSat *= (1f - 0.2f * filterIntensity)
            }
            "Mono" -> {
                baseSat = 0f
            }
            "Silvertone" -> {
                baseSat = 0f
                baseContrast *= (1f + 0.25f * filterIntensity)
            }
            "Noir" -> {
                baseSat = 0f
                baseContrast *= (1f + 0.5f * filterIntensity)
            }
        }

        if (isBlackAndWhite) {
            baseSat = 0f
        }

        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(baseSat.coerceIn(0f, 3f))

        val c = baseContrast.coerceIn(0.1f, 3f)
        val scale = (c * expFactor).coerceIn(0.1f, 3f)
        val contrastOffset = 128f * (1f - c)
        val brightnessOffset = (brightness / 100f) * 64f + (brilliance / 100f) * 32f

        val warmthShift = (warmth / 100f) * 30f
        val tintShift = (tint / 100f) * 20f

        val rOffset = contrastOffset + brightnessOffset + warmthShift + tintShift
        val gOffset = contrastOffset + brightnessOffset - tintShift
        val bOffset = contrastOffset + brightnessOffset - warmthShift + tintShift

        val adjustMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, rOffset,
                0f, scale, 0f, 0f, gOffset,
                0f, 0f, scale, 0f, bOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        adjustMatrix.timesAssign(satMatrix)
        return adjustMatrix
    }

    fun toJsonString(): String {
        val json = JSONObject()
        json.put("autoEnhanced", autoEnhanced)
        json.put("exposure", exposure.toDouble())
        json.put("brilliance", brilliance.toDouble())
        json.put("highlights", highlights.toDouble())
        json.put("shadows", shadows.toDouble())
        json.put("contrast", contrast.toDouble())
        json.put("brightness", brightness.toDouble())
        json.put("blackPoint", blackPoint.toDouble())
        json.put("saturation", saturation.toDouble())
        json.put("vibrance", vibrance.toDouble())
        json.put("warmth", warmth.toDouble())
        json.put("tint", tint.toDouble())
        json.put("isBlackAndWhite", isBlackAndWhite)
        json.put("filterName", filterName)
        json.put("filterIntensity", filterIntensity.toDouble())
        json.put("cropAspectRatio", cropAspectRatio)
        json.put("rotationDegrees", rotationDegrees.toDouble())
        json.put("isFlippedHorizontal", isFlippedHorizontal)
        return json.toString()
    }

    companion object {
        fun fromJson(str: String?): EditAdjustments {
            if (str.isNullOrBlank()) return EditAdjustments()
            return try {
                if (str.startsWith("{")) {
                    val obj = JSONObject(str)
                    EditAdjustments(
                        autoEnhanced = obj.optBoolean("autoEnhanced", false),
                        exposure = obj.optDouble("exposure", 0.0).toFloat(),
                        brilliance = obj.optDouble("brilliance", 0.0).toFloat(),
                        highlights = obj.optDouble("highlights", 0.0).toFloat(),
                        shadows = obj.optDouble("shadows", 0.0).toFloat(),
                        contrast = obj.optDouble("contrast", 0.0).toFloat(),
                        brightness = obj.optDouble("brightness", 0.0).toFloat(),
                        blackPoint = obj.optDouble("blackPoint", 0.0).toFloat(),
                        saturation = obj.optDouble("saturation", 0.0).toFloat(),
                        vibrance = obj.optDouble("vibrance", 0.0).toFloat(),
                        warmth = obj.optDouble("warmth", 0.0).toFloat(),
                        tint = obj.optDouble("tint", 0.0).toFloat(),
                        isBlackAndWhite = obj.optBoolean("isBlackAndWhite", false),
                        filterName = obj.optString("filterName", "Original"),
                        filterIntensity = obj.optDouble("filterIntensity", 1.0).toFloat(),
                        cropAspectRatio = obj.optString("cropAspectRatio", "Original"),
                        rotationDegrees = obj.optDouble("rotationDegrees", 0.0).toFloat(),
                        isFlippedHorizontal = obj.optBoolean("isFlippedHorizontal", false)
                    )
                } else {
                    val parts = str.split(",")
                    EditAdjustments(
                        exposure = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
                        contrast = parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
                        saturation = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
                        filterName = parts.getOrNull(3) ?: "Original"
                    )
                }
            } catch (e: Exception) {
                EditAdjustments()
            }
        }
    }
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
    val deletedTimestamp: Long? = null,
    val isScreenshot: Boolean = false,
    val isEdited: Boolean = false,
    val editAdjustments: EditAdjustments = EditAdjustments(),
    val customAlbumIds: Set<String> = emptySet(),
    val location: String = "Unknown Location",
    val cameraModel: String = "iPhone 16 Pro",
    val lensInfo: String = "24 mm f/1.78 ISO 50",
    val resolution: String = "48 MP • 8064 × 6048",
    val fileSize: String = "4.2 MB",
    val categoryTag: String = "Nature",
    val tripName: String? = null,
    val personOrPetName: String? = null
)
