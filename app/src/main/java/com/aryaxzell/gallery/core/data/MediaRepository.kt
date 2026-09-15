package com.aryaxzell.gallery.core.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.aryaxzell.gallery.core.data.db.AlbumEntity
import com.aryaxzell.gallery.core.data.db.AlbumMediaCrossRef
import com.aryaxzell.gallery.core.data.db.GalleryDao
import com.aryaxzell.gallery.core.data.db.MediaMetadataEntity
import com.aryaxzell.gallery.core.data.db.SearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaRepository(
    private val context: Context,
    private val dao: GalleryDao
) {
    private val _rawMediaItems = MutableStateFlow<List<MediaItem>>(getCuratedDemoMedia())
    val rawMediaItems = _rawMediaItems.asStateFlow()

    val metadataFlow: Flow<List<MediaMetadataEntity>> = dao.getAllMetadata()
    val albumsFlow: Flow<List<AlbumEntity>> = dao.getAllAlbums()
    val recentSearchesFlow: Flow<List<SearchHistoryEntity>> = dao.getRecentSearches()
    val albumCrossRefsFlow: Flow<List<AlbumMediaCrossRef>> = dao.getAllAlbumMediaCrossRefs()

    // Combined live media items merging MediaStore/Demo, Room custom metadata, and custom album memberships
    private val enrichedMediaItems: Flow<List<MediaItem>> = combine(
        _rawMediaItems,
        metadataFlow,
        albumCrossRefsFlow
    ) { items, metadataList, crossRefs ->
        val metadataMap = metadataList.associateBy { it.mediaId }
        val albumMap = mutableMapOf<Long, MutableSet<String>>()
        for (ref in crossRefs) {
            albumMap.getOrPut(ref.mediaId) { mutableSetOf() }.add(ref.albumId)
        }

        items.map { item ->
            val meta = metadataMap[item.id]
            val customAlbums = albumMap[item.id] ?: emptySet()
            if (meta != null) {
                item.copy(
                    isFavorite = meta.isFavorite,
                    isHidden = meta.isHidden,
                    isDeleted = meta.isDeleted,
                    deletedTimestamp = meta.deletedTimestamp,
                    isEdited = meta.isEdited,
                    editAdjustments = if (meta.isEdited) EditAdjustments.fromJson(meta.editJson) else item.editAdjustments,
                    customAlbumIds = customAlbums
                )
            } else {
                item.copy(customAlbumIds = customAlbums)
            }
        }
    }

    val allMediaItems: Flow<List<MediaItem>> = enrichedMediaItems.map { list ->
        list.filter { !it.isDeleted && !it.isHidden }
    }

    val hiddenMediaItems: Flow<List<MediaItem>> = enrichedMediaItems.map { list ->
        list.filter { it.isHidden && !it.isDeleted }
    }

    val deletedMediaItems: Flow<List<MediaItem>> = enrichedMediaItems.map { list ->
        list.filter { it.isDeleted }
    }

    suspend fun loadMedia() = withContext(Dispatchers.IO) {
        // Automatically purge trash older than 30 days
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
            dao.purgeDeletedOlderThan(thirtyDaysAgo)
        } catch (_: Exception) {
        }

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
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.Video.Media.DURATION
            )

            val queryUri = MediaStore.Files.getContentUri("external")
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT 500"

            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val dayFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            val cachedDateStrMap = HashMap<Long, Pair<String, String>>()

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
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

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
                    val durationMs = if (durCol != -1 && isVideo) cursor.getLong(durCol) else 0L

                    val contentUri = if (isVideo) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    // Key by day boundary to avoid formatting thousands of times
                    val dayKey = dateAddedMs / 86400000L
                    val (monthYear, dayStr) = cachedDateStrMap.getOrPut(dayKey) {
                        val d = Date(dateAddedMs)
                        Pair(monthFormat.format(d), dayFormat.format(d))
                    }

                    val resString = if (width > 0 && height > 0) {
                        val mp = (width.toLong() * height.toLong()) / 1_000_000.0
                        "${String.format(Locale.US, "%.1f", mp)} MP • $width × $height"
                    } else "Original Resolution"

                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            thumbnailUri = contentUri,
                            title = name,
                            dateAdded = dateAddedMs,
                            dateString = dayStr,
                            monthYear = monthYear,
                            dayString = dayStr,
                            duration = durationMs,
                            isVideo = isVideo,
                            isScreenshot = isScreenshot,
                            resolution = resString,
                            fileSize = "${String.format(Locale.US, "%.1f", sizeBytes / (1024f * 1024f))} MB",
                            cameraModel = if (isVideo) "Device Camcorder" else "Device Camera",
                            lensInfo = if (isVideo) "Video Recording" else "Standard Lens",
                            categoryTag = if (isVideo) "Videos" else if (isScreenshot) "Screenshots" else "Photos"
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // MediaStore permission might not be granted yet
        }

        // If no device media found, populate curated showcase
        if (mediaList.isEmpty()) {
            _rawMediaItems.value = getCuratedDemoMedia()
        } else {
            // If device has photos but no videos, append sample videos so video features are always testable
            if (mediaList.none { it.isVideo }) {
                val sampleVideos = getCuratedDemoMedia().filter { it.isVideo }
                mediaList.addAll(sampleVideos)
            }
            _rawMediaItems.value = mediaList
        }
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

    suspend fun toggleHidden(mediaId: Long, currentVal: Boolean) {
        val newVal = !currentVal
        val existing = dao.getMetadata(mediaId)
        if (existing != null) {
            dao.setHidden(mediaId, newVal)
        } else {
            dao.upsertMetadata(MediaMetadataEntity(mediaId = mediaId, isHidden = newVal))
        }
    }

    suspend fun setHiddenBatch(mediaIds: List<Long>, isHidden: Boolean) {
        mediaIds.forEach { id ->
            val existing = dao.getMetadata(id)
            if (existing != null) {
                dao.setHidden(id, isHidden)
            } else {
                dao.upsertMetadata(MediaMetadataEntity(mediaId = id, isHidden = isHidden))
            }
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

    suspend fun deleteMediaBatch(mediaIds: List<Long>) {
        val now = System.currentTimeMillis()
        mediaIds.forEach { id ->
            val existing = dao.getMetadata(id)
            if (existing != null) {
                dao.setDeleted(id, true, now)
            } else {
                dao.upsertMetadata(MediaMetadataEntity(mediaId = id, isDeleted = true, deletedTimestamp = now))
            }
        }
    }

    suspend fun restoreMedia(mediaId: Long) {
        dao.restoreMedia(mediaId)
    }

    suspend fun restoreMediaBatch(mediaIds: List<Long>) {
        dao.restoreMediaBatch(mediaIds)
    }

    suspend fun deletePermanently(mediaId: Long) {
        dao.deletePermanently(mediaId)
        dao.deleteCrossRefsForMedia(mediaId)
        _rawMediaItems.value = _rawMediaItems.value.filter { it.id != mediaId }
    }

    suspend fun deletePermanentlyBatch(mediaIds: List<Long>) {
        dao.deletePermanentlyBatch(mediaIds)
        mediaIds.forEach { dao.deleteCrossRefsForMedia(it) }
        val idSet = mediaIds.toSet()
        _rawMediaItems.value = _rawMediaItems.value.filter { !idSet.contains(it.id) }
    }

    suspend fun addMediaToAlbum(albumId: String, mediaId: Long) {
        dao.insertAlbumMediaCrossRef(AlbumMediaCrossRef(albumId = albumId, mediaId = mediaId))
    }

    suspend fun addMediaToAlbumBatch(albumId: String, mediaIds: List<Long>) {
        val refs = mediaIds.map { AlbumMediaCrossRef(albumId = albumId, mediaId = it) }
        dao.insertAlbumMediaCrossRefs(refs)
    }

    suspend fun removeMediaFromAlbum(albumId: String, mediaId: Long) {
        dao.removeMediaFromAlbum(albumId, mediaId)
    }

    suspend fun saveEdits(mediaId: Long, adjustments: EditAdjustments) {
        val existing = dao.getMetadata(mediaId)
        val json = adjustments.toJsonString()
        if (existing != null) {
            dao.saveEdits(mediaId, json)
        } else {
            dao.upsertMetadata(MediaMetadataEntity(mediaId = mediaId, isEdited = true, editJson = json))
        }

        // Also update in-memory raw item
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
        dao.deleteCrossRefsForAlbum(id)
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
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&q=80"),
                    title = "Yosemite Valley Reflections",
                    dateAdded = now - (0.2 * day).toLong(),
                    dateString = "Today, 14:24",
                    monthYear = "September 2026",
                    dayString = "Today",
                    location = "Yosemite National Park, CA",
                    cameraModel = "Sony Alpha 7R V",
                    lensInfo = "FE 24-70mm F2.8 GM II • 24 mm • f/8.0 • 1/125s • ISO 100",
                    resolution = "61.0 MP • 9504 × 6336",
                    fileSize = "18.4 MB",
                    categoryTag = "Nature",
                    tripName = "California Roadtrip"
                ),
                MediaItem(
                    id = 1002L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600&q=80"),
                    title = "Portrait of Emma",
                    dateAdded = now - (0.5 * day).toLong(),
                    dateString = "Today, 11:15",
                    monthYear = "September 2026",
                    dayString = "Today",
                    location = "SoHo, New York",
                    cameraModel = "Fujifilm GFX 100 II",
                    lensInfo = "GF 110mm F2 R LM WR • 110 mm • f/2.0 • 1/250s • ISO 160",
                    resolution = "102.0 MP • 11648 × 8736",
                    fileSize = "32.1 MB",
                    categoryTag = "Portraits",
                    personOrPetName = "Emma"
                ),
                MediaItem(
                    id = 1003L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=600&q=80"),
                    title = "Milo the Golden Retriever",
                    dateAdded = now - 1 * day,
                    dateString = "Yesterday, 16:40",
                    monthYear = "September 2026",
                    dayString = "Yesterday",
                    location = "Central Park, New York",
                    cameraModel = "Canon EOS R5 Mark II",
                    lensInfo = "RF 85mm F1.2L USM • 85 mm • f/1.4 • 1/1000s • ISO 200",
                    resolution = "45.0 MP • 8192 × 5464",
                    fileSize = "14.2 MB",
                    categoryTag = "Pets",
                    personOrPetName = "Milo"
                ),
                MediaItem(
                    id = 1004L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80"),
                    title = "Tropical Sunrise Horizon",
                    dateAdded = now - 2 * day,
                    dateString = "Sep 12, 2026, 06:12",
                    monthYear = "September 2026",
                    dayString = "September 12, 2026",
                    location = "Uluwatu, Bali",
                    cameraModel = "Google Pixel 9 Pro XL",
                    lensInfo = "Main 50MP • 24 mm • f/1.68 • 1/640s • ISO 50",
                    resolution = "50.0 MP • 8192 × 6144",
                    fileSize = "11.6 MB",
                    categoryTag = "Nature",
                    tripName = "Bali Escape",
                    isFavorite = true
                ),
                MediaItem(
                    id = 1005L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=600&q=80"),
                    title = "Golden Hour Selfie",
                    dateAdded = now - 3 * day,
                    dateString = "Sep 11, 2026, 18:30",
                    monthYear = "September 2026",
                    dayString = "September 11, 2026",
                    location = "Santa Monica Pier, CA",
                    cameraModel = "Samsung Galaxy S24 Ultra",
                    lensInfo = "Front Camera • 22 mm • f/2.2 • 1/200s • ISO 80",
                    resolution = "12.0 MP • 4000 × 3000",
                    fileSize = "3.8 MB",
                    categoryTag = "Selfies",
                    personOrPetName = "Sophia"
                ),
                MediaItem(
                    id = 1006L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600&q=80"),
                    title = "Artisan Garden Salad",
                    dateAdded = now - 5 * day,
                    dateString = "Sep 9, 2026, 13:05",
                    monthYear = "September 2026",
                    dayString = "September 9, 2026",
                    location = "Milan, Italy",
                    cameraModel = "Leica Q3",
                    lensInfo = "Summilux 28mm f/1.7 ASPH • f/2.8 • 1/160s • ISO 125",
                    resolution = "60.0 MP • 9520 × 6336",
                    fileSize = "22.5 MB",
                    categoryTag = "Food"
                ),
                MediaItem(
                    id = 1007L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=600&q=80"),
                    title = "Kyoto Bamboo Grove Walk",
                    dateAdded = now - 8 * day,
                    dateString = "Sep 6, 2026, 09:45",
                    monthYear = "September 2026",
                    dayString = "September 6, 2026",
                    location = "Arashiyama, Kyoto",
                    cameraModel = "Nikon Z8",
                    lensInfo = "NIKKOR Z 35mm f/1.8 S • 35 mm • f/4.0 • 1/80s • ISO 400",
                    resolution = "45.7 MP • 8256 × 5504",
                    fileSize = "16.8 MB",
                    categoryTag = "Nature",
                    tripName = "Kyoto Japan",
                    isFavorite = true
                ),
                MediaItem(
                    id = 1008L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=600&q=80"),
                    title = "Luna Sleeping in Sunbeam",
                    dateAdded = now - 12 * day,
                    dateString = "Sep 2, 2026, 15:20",
                    monthYear = "September 2026",
                    dayString = "September 2, 2026",
                    location = "Living Room",
                    cameraModel = "Apple iPhone 16 Pro Max",
                    lensInfo = "Main 48mm 2x Tele • f/1.78 • 1/60s • ISO 250",
                    resolution = "24.0 MP • 5712 × 4284",
                    fileSize = "4.5 MB",
                    categoryTag = "Pets",
                    personOrPetName = "Luna"
                ),
                MediaItem(
                    id = 1009L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=600&q=80"),
                    title = "Pacific Coast Highway Vista",
                    dateAdded = now - 20 * day,
                    dateString = "Aug 25, 2026, 17:10",
                    monthYear = "August 2026",
                    dayString = "August 25, 2026",
                    location = "Big Sur, CA",
                    cameraModel = "Hasselblad X2D 100C",
                    lensInfo = "XCD 38mm f/2.5 V • 38 mm • f/5.6 • 1/320s • ISO 64",
                    resolution = "100.0 MP • 11656 × 8742",
                    fileSize = "36.2 MB",
                    categoryTag = "Panoramas",
                    tripName = "California Roadtrip",
                    isFavorite = true
                ),
                MediaItem(
                    id = 1010L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=600&q=80"),
                    title = "Sunset Over Swiss Alps",
                    dateAdded = now - 25 * day,
                    dateString = "Aug 20, 2026, 19:40",
                    monthYear = "August 2026",
                    dayString = "August 20, 2026",
                    location = "Zermatt, Switzerland",
                    cameraModel = "Sony Alpha 1",
                    lensInfo = "FE 70-200mm F2.8 GM OSS II • 135 mm • f/4.0 • 1/500s • ISO 100",
                    resolution = "50.1 MP • 8640 × 5760",
                    fileSize = "19.8 MB",
                    categoryTag = "Nature",
                    tripName = "Swiss Alps"
                ),
                MediaItem(
                    id = 1011L,
                    uri = Uri.parse("https://images.unsplash.com/photo-1488161628813-04466f872be2?w=1200&q=80"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1488161628813-04466f872be2?w=600&q=80"),
                    title = "Leo by the Shoreline",
                    dateAdded = now - 35 * day,
                    dateString = "Aug 10, 2026, 16:15",
                    monthYear = "August 2026",
                    dayString = "August 10, 2026",
                    location = "Positano, Italy",
                    cameraModel = "Leica M11-P",
                    lensInfo = "APO-Summicron-M 50mm f/2 ASPH • f/2.8 • 1/1000s • ISO 64",
                    resolution = "60.3 MP • 9528 × 6328",
                    fileSize = "21.4 MB",
                    categoryTag = "Portraits",
                    personOrPetName = "Leo",
                    tripName = "Amalfi Coast"
                ),
                // Curated Video 1: 4K Drone
                MediaItem(
                    id = 1012L,
                    uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80"),
                    title = "Cinematic Drone Trail",
                    dateAdded = now - (0.3 * day).toLong(),
                    dateString = "Today, 12:10",
                    monthYear = "September 2026",
                    dayString = "Today",
                    duration = 15000L,
                    isVideo = true,
                    location = "Kauai, Hawaii",
                    cameraModel = "DJI Mavic 3 Cine",
                    lensInfo = "Hasselblad L2D-20c 24mm • f/2.8 • Apple ProRes 422 HQ",
                    resolution = "4K • 3840 × 2160 • 60 fps",
                    fileSize = "48.5 MB",
                    categoryTag = "Videos",
                    tripName = "Hawaii Expedition"
                ),
                // Curated Video 2: Nature Open Movie
                MediaItem(
                    id = 1013L,
                    uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&q=80"),
                    title = "Sunny Forest Meadow Reel",
                    dateAdded = now - 4 * day,
                    dateString = "Sep 10, 2026, 15:30",
                    monthYear = "September 2026",
                    dayString = "September 10, 2026",
                    duration = 60000L,
                    isVideo = true,
                    location = "Black Forest, Germany",
                    cameraModel = "Sony FX3 Cinema Line",
                    lensInfo = "FE C 16-35mm T3.1 G Cine • 28 mm • 4K 120fps",
                    resolution = "1080p HD • 1920 × 1080 • 60 fps",
                    fileSize = "32.0 MB",
                    categoryTag = "Videos",
                    isFavorite = true
                ),
                // Curated Video 3: Ocean Waves Coast
                MediaItem(
                    id = 1014L,
                    uri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
                    thumbnailUri = Uri.parse("https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=800&q=80"),
                    title = "Pacific Ocean Surf Break",
                    dateAdded = now - 15 * day,
                    dateString = "Aug 30, 2026, 17:45",
                    monthYear = "August 2026",
                    dayString = "August 30, 2026",
                    duration = 15000L,
                    isVideo = true,
                    location = "Pipeline Beach, Oahu",
                    cameraModel = "RED V-Raptor 8K",
                    lensInfo = "Canon CN-E 50mm T1.3 L F • 4K High Speed",
                    resolution = "4K • 3840 × 2160 • 60 fps",
                    fileSize = "52.3 MB",
                    categoryTag = "Videos"
                )
            )
        }
    }
}
