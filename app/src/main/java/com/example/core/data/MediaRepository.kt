package com.example.core.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.core.data.db.AlbumEntity
import com.example.core.data.db.GalleryDao
import com.example.core.data.db.MediaMetadataEntity
import com.example.core.data.db.SearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaRepository(
    private val context: Context,
    private val dao: GalleryDao
) {
    private val _rawMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val rawMediaItems = _rawMediaItems.asStateFlow()

    val metadataFlow: Flow<List<MediaMetadataEntity>> = dao.getAllMetadata()
    val albumsFlow: Flow<List<AlbumEntity>> = dao.getAllAlbums()
    val recentSearchesFlow: Flow<List<SearchHistoryEntity>> = dao.getRecentSearches()

    // Combined live media items merging MediaStore/Demo and Room custom metadata
    val allMediaItems: Flow<List<MediaItem>> = combine(_rawMediaItems, metadataFlow) { items, metadataList ->
        val metadataMap = metadataList.associateBy { it.mediaId }
        items.map { item ->
            val meta = metadataMap[item.id]
            if (meta != null) {
                item.copy(
                    isFavorite = meta.isFavorite,
                    isHidden = meta.isHidden,
                    isDeleted = meta.isDeleted,
                    isEdited = meta.isEdited
                )
            } else {
                item
            }
        }.filter { !it.isDeleted && !it.isHidden }
    }

    suspend fun loadMedia() = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Query Device MediaStore if permission granted
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE
            )

            val queryUri = MediaStore.Files.getContentUri("external")
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val dayFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Media $id"
                    val dateAddedSec = cursor.getLong(dateCol)
                    val dateAddedMs = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol) ?: ""
                    val isVideo = mime.startsWith("video")
                    val isScreenshot = name.contains("Screenshot", ignoreCase = true)

                    val contentUri = if (isVideo) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    val dateObj = Date(dateAddedMs)
                    val monthYear = monthFormat.format(dateObj)
                    val dayStr = dayFormat.format(dateObj)

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            title = name,
                            dateAdded = dateAddedMs,
                            dateString = dayStr,
                            monthYear = monthYear,
                            dayString = dayStr,
                            isVideo = isVideo,
                            isScreenshot = isScreenshot,
                            resolution = "$width × $height",
                            fileSize = "${"%.1f".format(sizeBytes / (1024f * 1024f))} MB"
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // MediaStore permission might not be granted yet
        }

        // 2. Always augment with or provide rich curated demo items so the user
        // in AI Studio streaming emulator has an instant, vibrant iOS 26 gallery experience
        if (mediaList.isEmpty()) {
            mediaList.addAll(getCuratedDemoMedia())
        }

        _rawMediaItems.value = mediaList
    }

    suspend fun toggleFavorite(mediaId: Long, currentVal: Boolean) {
        val newVal = !currentVal
        val existing = dao.getMetadata(mediaId)
        if (existing != null) {
            dao.setFavorite(mediaId, newVal)
        } else {
            dao.upsertMetadata(MediaMetadataEntity(mediaId = mediaId, isFavorite = newVal))
        }
    }

    suspend fun deleteMedia(mediaId: Long) {
        val existing = dao.getMetadata(mediaId)
        if (existing != null) {
            dao.setDeleted(mediaId, true, System.currentTimeMillis())
        } else {
            dao.upsertMetadata(MediaMetadataEntity(mediaId = mediaId, isDeleted = true, deletedTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun saveEdits(mediaId: Long, adjustments: EditAdjustments) {
        // Save adjustments into metadata
        val existing = dao.getMetadata(mediaId)
        val json = "${adjustments.exposure},${adjustments.contrast},${adjustments.saturation},${adjustments.filterName}"
        if (existing != null) {
            dao.saveEdits(mediaId, json)
        } else {
            dao.upsertMetadata(MediaMetadataEntity(mediaId = mediaId, isEdited = true, editJson = json))
        }

        // Also update the in-memory item for live responsiveness
        _rawMediaItems.value = _rawMediaItems.value.map {
            if (it.id == mediaId) {
                it.copy(isEdited = true, editAdjustments = adjustments)
            } else it
        }
    }

    suspend fun revertEdits(mediaId: Long) {
        dao.revertEdits(mediaId)
        _rawMediaItems.value = _rawMediaItems.value.map {
            if (it.id == mediaId) {
                it.copy(isEdited = false, editAdjustments = EditAdjustments())
            } else it
        }
    }

    suspend fun createAlbum(name: String, coverUri: String? = null) {
        dao.insertAlbum(
            AlbumEntity(
                id = "album_${System.currentTimeMillis()}",
                name = name,
                coverUri = coverUri
            )
        )
    }

    suspend fun deleteAlbum(id: String) {
        dao.deleteAlbum(id)
    }

    suspend fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            dao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun removeRecentSearch(id: Long) {
        dao.deleteSearch(id)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }

    fun loadCuratedDemo() {
        _rawMediaItems.value = getCuratedDemoMedia()
    }

    companion object {
        fun getCuratedDemoMedia(): List<MediaItem> {
            val now = System.currentTimeMillis()
            val day = 86400000L

            return listOf(
                MediaItem(
                    id = 1001L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
                    title = "Yosemite Valley Reflections",
                    dateAdded = now - (0.2 * day).toLong(),
                    dateString = "Today, 14:24",
                    monthYear = "September 2026",
                    dayString = "Today",
                    location = "Yosemite National Park, CA",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "24 mm f/1.78 ISO 50",
                    resolution = "48 MP • 8064 × 6048",
                    fileSize = "5.8 MB",
                    categoryTag = "Nature",
                    tripName = "California Roadtrip"
                ),
                MediaItem(
                    id = 1002L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1200&q=80"),
                    title = "Portrait of Emma",
                    dateAdded = now - (0.5 * day).toLong(),
                    dateString = "Today, 11:15",
                    monthYear = "September 2026",
                    dayString = "Today",
                    location = "SoHo, New York",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "77 mm f/2.8 ISO 125",
                    resolution = "48 MP • 8064 × 6048",
                    fileSize = "4.2 MB",
                    categoryTag = "Portraits",
                    personOrPetName = "Emma"
                ),
                MediaItem(
                    id = 1003L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=1200&q=80"),
                    title = "Milo the Golden Retriever",
                    dateAdded = now - 1 * day,
                    dateString = "Yesterday, 16:40",
                    monthYear = "September 2026",
                    dayString = "Yesterday",
                    location = "Central Park, New York",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "48 mm f/1.78 ISO 80",
                    resolution = "24 MP • 5712 × 4284",
                    fileSize = "3.6 MB",
                    categoryTag = "Pets",
                    personOrPetName = "Milo"
                ),
                MediaItem(
                    id = 1004L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                    title = "Tropical Sunrise Horizon",
                    dateAdded = now - 2 * day,
                    dateString = "Sep 12, 2026, 06:12",
                    monthYear = "September 2026",
                    dayString = "September 12, 2026",
                    location = "Uluwatu, Bali",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "13 mm f/2.2 ISO 32",
                    resolution = "48 MP • 8064 × 6048",
                    fileSize = "6.1 MB",
                    categoryTag = "Nature",
                    tripName = "Bali Escape",
                    isFavorite = true
                ),
                MediaItem(
                    id = 1005L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=1200&q=80"),
                    title = "Golden Hour Selfie",
                    dateAdded = now - 3 * day,
                    dateString = "Sep 11, 2026, 18:30",
                    monthYear = "September 2026",
                    dayString = "September 11, 2026",
                    location = "Santa Monica Pier, CA",
                    cameraModel = "iPhone 16 Pro TrueDepth",
                    lensInfo = "23 mm f/1.9 ISO 100",
                    resolution = "12 MP • 4032 × 3024",
                    fileSize = "2.9 MB",
                    categoryTag = "Selfies",
                    personOrPetName = "Sophia"
                ),
                MediaItem(
                    id = 1006L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200&q=80"),
                    title = "Artisan Garden Salad",
                    dateAdded = now - 5 * day,
                    dateString = "Sep 9, 2026, 13:05",
                    monthYear = "September 2026",
                    dayString = "September 9, 2026",
                    location = "Milan, Italy",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "24 mm f/1.78 ISO 64",
                    resolution = "24 MP • 5712 × 4284",
                    fileSize = "3.8 MB",
                    categoryTag = "Food"
                ),
                MediaItem(
                    id = 1007L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=1200&q=80"),
                    title = "Kyoto Bamboo Grove Walk",
                    dateAdded = now - 8 * day,
                    dateString = "Sep 6, 2026, 09:45",
                    monthYear = "September 2026",
                    dayString = "September 6, 2026",
                    location = "Arashiyama, Kyoto",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "24 mm f/1.78 ISO 200",
                    resolution = "48 MP • 8064 × 6048",
                    fileSize = "7.3 MB",
                    categoryTag = "Nature",
                    tripName = "Kyoto Japan",
                    isFavorite = true
                ),
                MediaItem(
                    id = 1008L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=1200&q=80"),
                    title = "Luna Sleeping in Sunbeam",
                    dateAdded = now - 12 * day,
                    dateString = "Sep 2, 2026, 15:20",
                    monthYear = "September 2026",
                    dayString = "September 2, 2026",
                    location = "Living Room",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "48 mm f/1.78 ISO 160",
                    resolution = "24 MP • 5712 × 4284",
                    fileSize = "3.2 MB",
                    categoryTag = "Pets",
                    personOrPetName = "Luna"
                ),
                MediaItem(
                    id = 1009L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1200&q=80"),
                    title = "Pacific Coast Highway Vista",
                    dateAdded = now - 20 * day,
                    dateString = "Aug 25, 2026, 17:10",
                    monthYear = "August 2026",
                    dayString = "August 25, 2026",
                    location = "Big Sur, CA",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "13 mm f/2.2 ISO 40",
                    resolution = "48 MP • 8064 × 6048",
                    fileSize = "6.9 MB",
                    categoryTag = "Panoramas",
                    tripName = "California Roadtrip",
                    isFavorite = true
                ),
                MediaItem(
                    id = 1010L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=1200&q=80"),
                    title = "Sunset Over Swiss Alps",
                    dateAdded = now - 25 * day,
                    dateString = "Aug 20, 2026, 19:40",
                    monthYear = "August 2026",
                    dayString = "August 20, 2026",
                    location = "Zermatt, Switzerland",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "120 mm f/2.8 ISO 320",
                    resolution = "48 MP • 8064 × 6048",
                    fileSize = "5.1 MB",
                    categoryTag = "Nature",
                    tripName = "Swiss Alps"
                ),
                MediaItem(
                    id = 1011L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1488161628813-04466f872be2?w=1200&q=80"),
                    title = "Leo by the Shoreline",
                    dateAdded = now - 35 * day,
                    dateString = "Aug 10, 2026, 16:15",
                    monthYear = "August 2026",
                    dayString = "August 10, 2026",
                    location = "Positano, Italy",
                    cameraModel = "iPhone 16 Pro",
                    lensInfo = "77 mm f/2.8 ISO 100",
                    resolution = "24 MP • 5712 × 4284",
                    fileSize = "4.0 MB",
                    categoryTag = "Portraits",
                    personOrPetName = "Leo",
                    tripName = "Amalfi Coast"
                ),
                MediaItem(
                    id = 1012L,
                    uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                    title = "Cinematic Drone Trail",
                    dateAdded = now - 40 * day,
                    dateString = "Aug 5, 2026, 14:00",
                    monthYear = "August 2026",
                    dayString = "August 5, 2026",
                    duration = 15000L,
                    isVideo = true,
                    location = "Kauai, Hawaii",
                    cameraModel = "iPhone 16 Pro 4K HDR",
                    lensInfo = "24 mm f/1.78 ProRes",
                    resolution = "4K • 3840 × 2160 • 60 fps",
                    fileSize = "48.5 MB",
                    categoryTag = "Videos"
                )
            )
        }
    }
}
