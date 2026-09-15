package com.aryaxzell.gallery.feature

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aryaxzell.gallery.core.common.GallerySettings
import com.aryaxzell.gallery.core.common.GallerySettingsRepository
import com.aryaxzell.gallery.core.data.EditAdjustments
import com.aryaxzell.gallery.core.data.MediaItem
import com.aryaxzell.gallery.core.data.MediaRepository
import com.aryaxzell.gallery.core.data.db.AlbumEntity
import com.aryaxzell.gallery.core.data.db.GalleryDatabase
import com.aryaxzell.gallery.core.data.db.SearchHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilterConfig(
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isFilterSheetOpen: Boolean = false,
    val filterOnlyEdited: Boolean = false,
    val filterHideScreenshots: Boolean = false,
    val filterOnlyVideos: Boolean = false,
    val filterOnlyPhotos: Boolean = false,
    val gridColumns: Int = 3,
    val searchQuery: String = "",
    val searchFilterChip: String = "All"
)

data class GalleryUiState(
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val hiddenMedia: List<MediaItem> = emptyList(),
    val deletedMedia: List<MediaItem> = emptyList(),
    val customAlbums: List<AlbumEntity> = emptyList(),
    val recentSearches: List<SearchHistoryEntity> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isFilterSheetOpen: Boolean = false,
    val filterOnlyEdited: Boolean = false,
    val filterHideScreenshots: Boolean = false,
    val filterOnlyVideos: Boolean = false,
    val filterOnlyPhotos: Boolean = false,
    val gridColumns: Int = 3,
    val searchQuery: String = "",
    val searchFilterChip: String = "All", // All, Photos, Videos, Screenshots, Favorites
    val activeDetailItem: MediaItem? = null,
    val activeDetailList: List<MediaItem>? = null,
    val activeEditItem: MediaItem? = null,
    val currentEdits: EditAdjustments = EditAdjustments(),
    val activeMemoryPhotos: List<MediaItem>? = null,
    val activeAlbumDetail: Pair<String, List<MediaItem>>? = null,
    val settings: GallerySettings = GallerySettings(),
    val isSpatialMode: Boolean = false,
    val isLoading: Boolean = false
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GalleryDatabase.getInstance(application)
    private val repository = MediaRepository(application, database.galleryDao())
    private val settingsRepository = GallerySettingsRepository(application)

    private val _filterConfig = MutableStateFlow(FilterConfig())
    private val _activeDetailItem = MutableStateFlow<MediaItem?>(null)
    private val _activeDetailList = MutableStateFlow<List<MediaItem>?>(null)
    private val _activeEditItem = MutableStateFlow<MediaItem?>(null)
    private val _currentEdits = MutableStateFlow(EditAdjustments())
    private val _activeMemoryPhotos = MutableStateFlow<List<MediaItem>?>(null)
    private val _activeAlbumName = MutableStateFlow<String?>(null)
    private val _activeAlbumCustomItems = MutableStateFlow<List<MediaItem>?>(null)
    private val _settings = MutableStateFlow(GallerySettings())
    private val _isSpatialMode = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)

    private data class BaseData(
        val allMedia: List<MediaItem>,
        val filteredMedia: List<MediaItem>,
        val hiddenMedia: List<MediaItem>,
        val deletedMedia: List<MediaItem>,
        val customAlbums: List<AlbumEntity>,
        val recentSearches: List<SearchHistoryEntity>,
        val config: FilterConfig
    )

    private data class ActiveInteractions(
        val detailItem: MediaItem?,
        val detailList: List<MediaItem>?,
        val editItem: MediaItem?,
        val currentEdits: EditAdjustments,
        val memoryPhotos: List<MediaItem>?
    )

    private data class EnvironmentState(
        val albumName: String?,
        val albumCustomItems: List<MediaItem>?,
        val settings: GallerySettings,
        val isSpatialMode: Boolean,
        val isLoading: Boolean
    )

    private val mediaStreamsFlow = combine(
        repository.allMediaItems,
        repository.hiddenMediaItems,
        repository.deletedMediaItems
    ) { all, hidden, deleted ->
        Triple(all, hidden, deleted)
    }

    private val baseDataFlow = combine(
        mediaStreamsFlow,
        repository.albumsFlow,
        repository.recentSearchesFlow,
        _filterConfig
    ) { (media, hidden, deleted), albums, searches, config ->
        var filtered = media

        if (config.filterOnlyEdited) {
            filtered = filtered.filter { it.isEdited }
        }
        if (config.filterHideScreenshots) {
            filtered = filtered.filter { !it.isScreenshot }
        }
        if (config.filterOnlyVideos) {
            filtered = filtered.filter { it.isVideo }
        }
        if (config.filterOnlyPhotos) {
            filtered = filtered.filter { !it.isVideo }
        }

        when (config.searchFilterChip) {
            "Photos" -> filtered = filtered.filter { !it.isVideo }
            "Videos" -> filtered = filtered.filter { it.isVideo }
            "Screenshots" -> filtered = filtered.filter { it.isScreenshot }
            "Favorites" -> filtered = filtered.filter { it.isFavorite }
        }

        if (config.searchQuery.isNotBlank()) {
            val q = config.searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                it.location.lowercase().contains(q) ||
                it.categoryTag.lowercase().contains(q) ||
                it.dayString.lowercase().contains(q) ||
                (it.personOrPetName?.lowercase()?.contains(q) ?: false) ||
                (it.tripName?.lowercase()?.contains(q) ?: false)
            }
        }

        BaseData(
            allMedia = media,
            filteredMedia = filtered,
            hiddenMedia = hidden,
            deletedMedia = deleted,
            customAlbums = albums,
            recentSearches = searches,
            config = config
        )
    }

    private val activeInteractionsFlow = combine(
        _activeDetailItem,
        _activeDetailList,
        _activeEditItem,
        _currentEdits,
        _activeMemoryPhotos
    ) { detail, list, edit, edits, memory ->
        ActiveInteractions(detail, list, edit, edits, memory)
    }

    private val environmentFlow = combine(
        _activeAlbumName,
        _activeAlbumCustomItems,
        _settings,
        _isSpatialMode,
        _isLoading
    ) { albumName, albumCustom, settings, spatial, loading ->
        EnvironmentState(albumName, albumCustom, settings, spatial, loading)
    }

    val uiState: StateFlow<GalleryUiState> = combine(
        baseDataFlow,
        activeInteractionsFlow,
        environmentFlow
    ) { base, active, env ->
        val resolvedAlbumDetail = env.albumName?.let { name ->
            val items = when (name) {
                "Hidden" -> base.hiddenMedia
                "Recently Deleted" -> base.deletedMedia
                "Videos" -> base.allMedia.filter { it.isVideo }
                "Favorites" -> base.allMedia.filter { it.isFavorite }
                "Selfies" -> base.allMedia.filter { it.categoryTag == "Selfies" }
                "Portraits" -> base.allMedia.filter { it.categoryTag == "Portraits" }
                "Screenshots" -> base.allMedia.filter { it.isScreenshot }
                "Panoramas" -> base.allMedia.filter { it.categoryTag == "Panoramas" }
                "Imports" -> base.allMedia
                else -> env.albumCustomItems ?: base.allMedia
            }
            Pair(name, items)
        }

        GalleryUiState(
            allMedia = base.allMedia,
            filteredMedia = base.filteredMedia,
            hiddenMedia = base.hiddenMedia,
            deletedMedia = base.deletedMedia,
            customAlbums = base.customAlbums,
            recentSearches = base.recentSearches,
            isSelectionMode = base.config.isSelectionMode,
            selectedIds = base.config.selectedIds,
            isFilterSheetOpen = base.config.isFilterSheetOpen,
            filterOnlyEdited = base.config.filterOnlyEdited,
            filterHideScreenshots = base.config.filterHideScreenshots,
            filterOnlyVideos = base.config.filterOnlyVideos,
            filterOnlyPhotos = base.config.filterOnlyPhotos,
            gridColumns = base.config.gridColumns,
            searchQuery = base.config.searchQuery,
            searchFilterChip = base.config.searchFilterChip,
            activeDetailItem = active.detailItem,
            activeDetailList = active.detailList,
            activeEditItem = active.editItem,
            currentEdits = active.currentEdits,
            activeMemoryPhotos = active.memoryPhotos,
            activeAlbumDetail = resolvedAlbumDetail,
            settings = env.settings,
            isSpatialMode = env.isSpatialMode,
            isLoading = env.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { persisted ->
                _settings.value = persisted
            }
        }
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.loadMedia()
            _isLoading.value = false
        }
    }

    fun loadDemoGallery() {
        viewModelScope.launch {
            repository.loadCuratedDemo()
        }
    }

    fun toggleSelectionMode() {
        _filterConfig.value = _filterConfig.value.let {
            val next = !it.isSelectionMode
            it.copy(
                isSelectionMode = next,
                selectedIds = if (!next) emptySet() else it.selectedIds
            )
        }
    }

    fun toggleItemSelection(id: Long) {
        _filterConfig.value = _filterConfig.value.let {
            val set = it.selectedIds.toMutableSet()
            if (set.contains(id)) set.remove(id) else set.add(id)
            it.copy(selectedIds = set)
        }
    }

    fun selectAll() {
        _filterConfig.value = _filterConfig.value.copy(
            selectedIds = uiState.value.filteredMedia.map { it.id }.toSet()
        )
    }

    fun clearSelection() {
        _filterConfig.value = _filterConfig.value.copy(
            selectedIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun deleteSelected() {
        val ids = _filterConfig.value.selectedIds.toList()
        viewModelScope.launch {
            repository.deleteMediaBatch(ids)
            clearSelection()
        }
    }

    fun deleteMedia(id: Long) {
        viewModelScope.launch {
            repository.deleteMedia(id)
            if (_activeDetailItem.value?.id == id) {
                _activeDetailItem.value = null
            }
        }
    }

    fun toggleHidden(id: Long, currentHidden: Boolean) {
        viewModelScope.launch {
            repository.toggleHidden(id, currentHidden)
            if (_activeDetailItem.value?.id == id) {
                _activeDetailItem.value = _activeDetailItem.value?.copy(isHidden = !currentHidden)
            }
        }
    }

    fun hideSelected() {
        val ids = _filterConfig.value.selectedIds.toList()
        viewModelScope.launch {
            repository.setHiddenBatch(ids, true)
            clearSelection()
        }
    }

    fun unhideSelected() {
        val ids = _filterConfig.value.selectedIds.toList()
        viewModelScope.launch {
            repository.setHiddenBatch(ids, false)
            clearSelection()
        }
    }

    fun restoreMedia(id: Long) {
        viewModelScope.launch {
            repository.restoreMedia(id)
            if (_activeDetailItem.value?.id == id) {
                _activeDetailItem.value = _activeDetailItem.value?.copy(isDeleted = false)
            }
        }
    }

    fun restoreSelected(targetIds: List<Long>? = null) {
        val ids = targetIds ?: _filterConfig.value.selectedIds.toList()
        viewModelScope.launch {
            repository.restoreMediaBatch(ids)
            clearSelection()
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            repository.deletePermanently(id)
            if (_activeDetailItem.value?.id == id) {
                _activeDetailItem.value = null
            }
        }
    }

    fun deletePermanentlySelected(targetIds: List<Long>? = null) {
        val ids = targetIds ?: _filterConfig.value.selectedIds.toList()
        viewModelScope.launch {
            repository.deletePermanentlyBatch(ids)
            clearSelection()
        }
    }

    fun addMediaToAlbum(albumId: String, mediaId: Long) {
        viewModelScope.launch {
            repository.addMediaToAlbum(albumId, mediaId)
        }
    }

    fun addSelectedToAlbum(albumId: String) {
        val ids = _filterConfig.value.selectedIds.toList()
        viewModelScope.launch {
            repository.addMediaToAlbumBatch(albumId, ids)
            clearSelection()
        }
    }

    fun removeMediaFromAlbum(albumId: String, mediaId: Long) {
        viewModelScope.launch {
            repository.removeMediaFromAlbum(albumId, mediaId)
        }
    }

    fun createAlbumWithMedia(name: String, mediaIds: List<Long>) {
        viewModelScope.launch {
            val albumId = "album_${System.currentTimeMillis()}"
            repository.createAlbum(name)
            if (mediaIds.isNotEmpty()) {
                repository.addMediaToAlbumBatch(albumId, mediaIds)
            }
            clearSelection()
        }
    }

    fun favoriteSelected() {
        viewModelScope.launch {
            _filterConfig.value.selectedIds.forEach { id ->
                repository.toggleFavorite(id, false)
            }
            clearSelection()
        }
    }

    fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, currentFavorite)
            if (_activeDetailItem.value?.id == id) {
                _activeDetailItem.value = _activeDetailItem.value?.copy(isFavorite = !currentFavorite)
            }
        }
    }

    fun setFilterSheetOpen(open: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(isFilterSheetOpen = open)
    }

    fun setFilterOnlyEdited(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterOnlyEdited = enabled)
    }

    fun setFilterHideScreenshots(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterHideScreenshots = enabled)
    }

    fun setFilterOnlyVideos(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterOnlyVideos = enabled)
    }

    fun setFilterOnlyPhotos(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterOnlyPhotos = enabled)
    }

    fun toggleGridDensity() {
        _filterConfig.value = _filterConfig.value.let {
            it.copy(gridColumns = if (it.gridColumns == 3) 5 else 3)
        }
    }

    fun setSearchQuery(query: String) {
        _filterConfig.value = _filterConfig.value.copy(searchQuery = query)
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.addRecentSearch(query)
            }
        }
    }

    fun setSearchFilterChip(chip: String) {
        _filterConfig.value = _filterConfig.value.copy(searchFilterChip = chip)
    }

    fun removeRecentSearch(id: Long) {
        viewModelScope.launch {
            repository.removeRecentSearch(id)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun openDetail(item: MediaItem, browsingList: List<MediaItem>? = null) {
        _activeDetailItem.value = item
        _activeDetailList.value = browsingList
    }

    fun setActiveDetailItem(item: MediaItem) {
        _activeDetailItem.value = item
    }

    fun closeDetail() {
        _activeDetailItem.value = null
        _activeDetailList.value = null
        _isSpatialMode.value = false
    }

    fun toggleSpatialMode() {
        _isSpatialMode.value = !_isSpatialMode.value
    }

    fun openEdit(item: MediaItem) {
        _activeEditItem.value = item
        _currentEdits.value = item.editAdjustments
    }

    fun closeEdit() {
        _activeEditItem.value = null
    }

    fun updateEdits(transform: (EditAdjustments) -> EditAdjustments) {
        _currentEdits.value = transform(_currentEdits.value)
    }

    fun saveEdits() {
        val active = _activeEditItem.value ?: return
        viewModelScope.launch {
            repository.saveEdits(active.id, _currentEdits.value)
            _activeDetailItem.value = active.copy(
                isEdited = true,
                editAdjustments = _currentEdits.value
            )
            _activeEditItem.value = null
        }
    }

    fun revertEdits() {
        val active = _activeEditItem.value ?: return
        viewModelScope.launch {
            repository.revertEdits(active.id)
            _currentEdits.value = EditAdjustments()
            _activeDetailItem.value = active.copy(
                isEdited = false,
                editAdjustments = EditAdjustments()
            )
        }
    }

    fun openMemory(items: List<MediaItem>) {
        _activeMemoryPhotos.value = items
    }

    fun closeMemory() {
        _activeMemoryPhotos.value = null
    }

    fun openAlbumDetail(name: String, items: List<MediaItem>) {
        _activeAlbumName.value = name
        _activeAlbumCustomItems.value = items
    }

    fun closeAlbumDetail() {
        _activeAlbumName.value = null
        _activeAlbumCustomItems.value = null
    }

    fun createAlbum(name: String) {
        viewModelScope.launch {
            repository.createAlbum(name)
        }
    }

    fun deleteAlbum(id: String) {
        viewModelScope.launch {
            repository.deleteAlbum(id)
        }
    }

    fun updateSettings(reduceTransparency: Boolean, reduceMotion: Boolean, liquidGlassEnabled: Boolean) {
        _settings.value = GallerySettings(
            reduceTransparency = reduceTransparency,
            reduceMotion = reduceMotion,
            liquidGlassEnabled = liquidGlassEnabled
        )
        viewModelScope.launch {
            settingsRepository.updateSettings(reduceTransparency, reduceMotion, liquidGlassEnabled)
        }
    }
}
