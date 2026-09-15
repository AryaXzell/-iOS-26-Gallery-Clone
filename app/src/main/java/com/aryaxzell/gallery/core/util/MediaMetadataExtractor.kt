package com.aryaxzell.gallery.core.util

import android.content.Context
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.aryaxzell.gallery.core.data.MediaItem
import java.io.InputStream
import java.util.Locale

object MediaMetadataExtractor {

    data class DetailedMetadata(
        val cameraModel: String,
        val lensInfo: String,
        val resolution: String,
        val fileSize: String,
        val location: String,
        val durationMs: Long,
        val formatBadge: String,
        val extraSpecs: String
    )

    fun extract(context: Context, item: MediaItem): DetailedMetadata {
        return try {
            if (item.isVideo) {
                extractVideo(context, item)
            } else {
                extractPhoto(context, item)
            }
        } catch (_: Exception) {
            fallbackMetadata(item)
        }
    }

    private fun extractPhoto(context: Context, item: MediaItem): DetailedMetadata {
        var make: String? = null
        var model: String? = null
        var focalLength: Double? = null
        var fNumber: Double? = null
        var iso: String? = null
        var exposureTime: Double? = null
        var width = 0
        var height = 0
        var latLong: FloatArray? = null

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(item.uri)
            if (inputStream != null) {
                inputStream.use { stream ->
                    val exif = ExifInterface(stream)
                    make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                    model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                    val fLen = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                    if (fLen > 0) focalLength = fLen
                    val fNum = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                    if (fNum > 0) fNumber = fNum
                    iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                    val expTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                    if (expTime > 0) exposureTime = expTime

                    width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

                    val coords = FloatArray(2)
                    if (exif.getLatLong(coords)) {
                        latLong = coords
                    }
                }
            }
        } catch (_: Exception) {
        }

        val resolvedCamera = when {
            !model.isNullOrBlank() -> {
                if (!make.isNullOrBlank() && !model!!.contains(make!!, ignoreCase = true)) {
                    "$make $model"
                } else {
                    model!!
                }
            }
            item.cameraModel.isNotBlank() && item.cameraModel != "Unknown Camera" -> item.cameraModel
            else -> "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        }

        val lensParts = mutableListOf<String>()
        if (focalLength != null && focalLength!! > 0) {
            lensParts.add("${focalLength!!.toInt()} mm")
        }
        if (fNumber != null && fNumber!! > 0) {
            lensParts.add("f/${String.format(Locale.US, "%.1f", fNumber)}")
        }
        if (exposureTime != null && exposureTime!! > 0) {
            if (exposureTime!! < 1.0) {
                val denom = (1.0 / exposureTime!!).toInt()
                lensParts.add("1/${denom}s")
            } else {
                lensParts.add("${String.format(Locale.US, "%.1f", exposureTime)}s")
            }
        }
        if (!iso.isNullOrBlank()) {
            lensParts.add("ISO $iso")
        }

        val resolvedLens = if (lensParts.isNotEmpty()) {
            lensParts.joinToString(" • ")
        } else {
            item.lensInfo
        }

        val resolvedRes = if (width > 0 && height > 0) {
            val mp = (width.toLong() * height.toLong()) / 1_000_000.0
            "${String.format(Locale.US, "%.1f", mp)} MP • $width × $height"
        } else {
            item.resolution
        }

        val resolvedLocation = if (latLong != null) {
            "${String.format(Locale.US, "%.4f", latLong!![0])}°, ${String.format(Locale.US, "%.4f", latLong!![1])}°"
        } else {
            item.location
        }

        val formatBadge = when {
            item.title.endsWith(".heic", ignoreCase = true) || item.title.endsWith(".heif", ignoreCase = true) -> "HEIF"
            item.title.endsWith(".png", ignoreCase = true) -> "PNG"
            item.title.endsWith(".dng", ignoreCase = true) || item.title.endsWith(".raw", ignoreCase = true) -> "RAW"
            else -> "JPEG"
        }

        return DetailedMetadata(
            cameraModel = resolvedCamera,
            lensInfo = resolvedLens,
            resolution = resolvedRes,
            fileSize = item.fileSize,
            location = resolvedLocation,
            durationMs = 0L,
            formatBadge = formatBadge,
            extraSpecs = if (item.isEdited) "Edited with Liquid Filters" else "Original"
        )
    }

    private fun extractVideo(context: Context, item: MediaItem): DetailedMetadata {
        var width = 0
        var height = 0
        var duration = item.duration
        var bitrate: String? = null
        var frameRate: String? = null
        var locStr: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, item.uri)
            val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            locStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            }

            width = wStr?.toIntOrNull() ?: 0
            height = hStr?.toIntOrNull() ?: 0
            val parsedDur = durStr?.toLongOrNull() ?: 0L
            if (parsedDur > 0) duration = parsedDur

            if (brStr != null) {
                val mbps = (brStr.toLongOrNull() ?: 0L) / 1_000_000f
                if (mbps > 0) bitrate = "${String.format(Locale.US, "%.1f", mbps)} Mbps"
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val resolvedRes = if (width > 0 && height > 0) {
            val tier = when {
                width >= 3840 || height >= 2160 -> "4K"
                width >= 1920 || height >= 1080 -> "1080p HD"
                else -> "${width}p"
            }
            val fpsPart = if (!frameRate.isNullOrBlank() && (frameRate.toFloatOrNull() ?: 0f) > 0f) {
                " • ${frameRate.toFloat().toInt()} fps"
            } else ""
            "$tier • $width × $height$fpsPart"
        } else {
            item.resolution
        }

        val formatBadge = when {
            item.title.endsWith(".mov", ignoreCase = true) -> "QuickTime MOV"
            else -> "H.264 / MP4"
        }

        val extraSpecs = buildList {
            if (!bitrate.isNullOrBlank()) add(bitrate)
            add("Stereo Audio")
            if (item.isEdited) add("Edited")
        }.joinToString(" • ")

        return DetailedMetadata(
            cameraModel = item.cameraModel.ifBlank { "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}" },
            lensInfo = if (item.lensInfo.isNotBlank() && item.lensInfo != "Unknown Lens") item.lensInfo else "Video Recording Mode",
            resolution = resolvedRes,
            fileSize = item.fileSize,
            location = locStr?.trim()?.takeIf { it.isNotBlank() } ?: item.location,
            durationMs = duration,
            formatBadge = formatBadge,
            extraSpecs = extraSpecs
        )
    }

    private fun fallbackMetadata(item: MediaItem): DetailedMetadata {
        return DetailedMetadata(
            cameraModel = item.cameraModel,
            lensInfo = item.lensInfo,
            resolution = item.resolution,
            fileSize = item.fileSize,
            location = item.location,
            durationMs = item.duration,
            formatBadge = if (item.isVideo) "MP4" else "JPEG",
            extraSpecs = "Standard"
        )
    }
}
