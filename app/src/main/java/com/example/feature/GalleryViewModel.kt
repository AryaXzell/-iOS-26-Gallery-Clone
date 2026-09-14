package com.example.feature

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.GallerySettings
import com.example.core.data.EditAdjustments
import com.example.core.data.MediaItem
import com.example.core.data.MediaRepository
import com.example.core.data.db.AlbumEntity
import com.example.core.data.db.GalleryDatabase
import com.example.core.data.db.SearchHistoryEntity
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
    val gridColumns: Int = 3,
    val searchQuery: String = "",
    val searchFilterChip: String = "All"
)

data class GalleryUiState(
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val customAlbums: List<AlbumEntity> = emptyList(),
    val recentSearches: List<SearchHistoryEntity> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isFilterSheetOpen: Boolean = false,
    val filterOnlyEdited: Boolean = false,
    val filterHideScreenshots: Boolean = false,
    val gridColumns: Int = 3,
    val searchQuery: String = "",
    val searchFilterChip: String = "All", // All, Photos, Videos, Screenshots, Favorites
    val activeDetailItem: MediaItem? = null,
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

    private val _filterConfig = MutableStateFlow(FilterConfig())
    private val _activeDetailItem = MutableStateFlow<MediaItem?>(null)
    private val _activeEditItem = MutableStateFlow<MediaItem?>(null)
    private val _currentEdits = MutableStateFlow(EditAdjustments())
    private val _activeMemoryPhotos = MutableStateFlow<List<MediaItem>?>(null)
    private val _activeAlbumDetail = MutableStateFlow<Pair<String, List<MediaItem>>?>(null)
    private val _settings = MutableStateFlow(GallerySettings())
    private val _isSpatialMode = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.allMediaItems,
        repository.albumsFlow,
        repository.recentSearchesFlow,
        _filterConfig
    ) { media, albums, searches, config ->
        var filtered = media

        if (config.filterOnlyEdited) {
            filtered = filtered.filter { it.isEdited }
        }
        if (config.filterHideScreenshots) {
            filtered = filtered.filter { !it.isScreenshot }
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

        GalleryUiState(
            allMedia = media,
            filteredMedia = filtered,
            customAlbums = albums,
            recentSearches = searches,
            isSelectionMode = config.isSelectionMode,
            selectedIds = config.selectedIds,
            isFilterSheetOpen = config.isFilterSheetOpen,
            filterOnlyEdited = config.filterOnlyEdited,
            filterHideScreenshots = config.filterHideScreenshots,
            gridColumns = config.gridColumns,
            searchQuery = config.searchQuery,
            searchFilterChip = config.searchFilterChip,
            activeDetailItem = _activeDetailItem.value,
            activeEditItem = _activeEditItem.value,
            currentEdits = _currentEdits.value,
            activeMemoryPhotos = _activeMemoryPhotos.value,
            activeAlbumDetail = _activeAlbumDetail.value,
            settings = _settings.value,
            isSpatialMode = _isSpatialMode.value,
            isLoading = _isLoading.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    init {
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
        viewModelScope.launch {
            _filterConfig.value.selectedIds.forEach { id ->
                repository.deleteMedia(id)
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

    fun openDetail(item: MediaItem) {
        _activeDetailItem.value = item
    }

    fun closeDetail() {
        _activeDetailItem.value = null
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
        _activeAlbumDetail.value = Pair(name, items)
    }

    fun closeAlbumDetail() {
        _activeAlbumDetail.value = null
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

    fun updateSettings(reduceTransparency: Boolean, reduceMotion: Boolean) {
        _settings.value = GallerySettings(
            reduceTransparency = reduceTransparency,
            reduceMotion = reduceMotion
        )
    }
}
