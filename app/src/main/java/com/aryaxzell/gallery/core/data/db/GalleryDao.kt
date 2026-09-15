package com.aryaxzell.gallery.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM media_metadata")
    fun getAllMetadata(): Flow<List<MediaMetadataEntity>>

    @Query("SELECT * FROM media_metadata WHERE mediaId = :id")
    suspend fun getMetadata(id: Long): MediaMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: MediaMetadataEntity)

    @Query("UPDATE media_metadata SET isFavorite = :isFavorite WHERE mediaId = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_metadata SET isHidden = :isHidden WHERE mediaId = :id")
    suspend fun setHidden(id: Long, isHidden: Boolean)

    @Query("UPDATE media_metadata SET isHidden = :isHidden WHERE mediaId IN (:ids)")
    suspend fun setHiddenBatch(ids: List<Long>, isHidden: Boolean)

    @Query("UPDATE media_metadata SET isDeleted = :isDeleted, deletedTimestamp = :timestamp WHERE mediaId = :id")
    suspend fun setDeleted(id: Long, isDeleted: Boolean, timestamp: Long?)

    @Query("UPDATE media_metadata SET isDeleted = :isDeleted, deletedTimestamp = :timestamp WHERE mediaId IN (:ids)")
    suspend fun setDeletedBatch(ids: List<Long>, isDeleted: Boolean, timestamp: Long?)

    @Query("UPDATE media_metadata SET isDeleted = 0, deletedTimestamp = NULL WHERE mediaId = :id")
    suspend fun restoreMedia(id: Long)

    @Query("UPDATE media_metadata SET isDeleted = 0, deletedTimestamp = NULL WHERE mediaId IN (:ids)")
    suspend fun restoreMediaBatch(ids: List<Long>)

    @Query("DELETE FROM media_metadata WHERE mediaId = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM media_metadata WHERE mediaId IN (:ids)")
    suspend fun deletePermanentlyBatch(ids: List<Long>)

    @Query("DELETE FROM media_metadata WHERE isDeleted = 1 AND deletedTimestamp IS NOT NULL AND deletedTimestamp < :threshold")
    suspend fun purgeDeletedOlderThan(threshold: Long)

    @Query("UPDATE media_metadata SET isEdited = 1, editJson = :json WHERE mediaId = :id")
    suspend fun saveEdits(id: Long, json: String)

    @Query("UPDATE media_metadata SET isEdited = 0, editJson = null WHERE mediaId = :id")
    suspend fun revertEdits(id: Long)

    // Albums
    @Query("SELECT * FROM custom_albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("DELETE FROM custom_albums WHERE id = :id")
    suspend fun deleteAlbum(id: String)

    // Album Media Cross References
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumMediaCrossRef(crossRef: AlbumMediaCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumMediaCrossRefs(crossRefs: List<AlbumMediaCrossRef>)

    @Query("DELETE FROM album_media_cross_ref WHERE albumId = :albumId AND mediaId = :mediaId")
    suspend fun removeMediaFromAlbum(albumId: String, mediaId: Long)

    @Query("DELETE FROM album_media_cross_ref WHERE albumId = :albumId")
    suspend fun deleteCrossRefsForAlbum(albumId: String)

    @Query("DELETE FROM album_media_cross_ref WHERE mediaId = :mediaId")
    suspend fun deleteCrossRefsForMedia(mediaId: Long)

    @Query("SELECT * FROM album_media_cross_ref")
    fun getAllAlbumMediaCrossRefs(): Flow<List<AlbumMediaCrossRef>>

    @Query("SELECT mediaId FROM album_media_cross_ref WHERE albumId = :albumId")
    fun getMediaIdsForAlbum(albumId: String): Flow<List<Long>>

    // Search History
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(query: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearch(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
