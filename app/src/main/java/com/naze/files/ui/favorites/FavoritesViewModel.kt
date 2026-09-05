package com.naze.files.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.files.data.favorites.FavoritesRepository
import com.naze.files.data.model.FileItem
import com.naze.files.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoriteEntry(val path: String, val item: FileItem?)

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _entries = MutableStateFlow<List<FavoriteEntry>>(emptyList())
    val entries: StateFlow<List<FavoriteEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favoritePaths.collect { paths ->
                _isLoading.value = true
                val list = paths.sorted().map { path -> FavoriteEntry(path, fileRepository.stat(path)) }
                _entries.value = list
                _isLoading.value = false
            }
        }
    }

    fun removeFavorite(path: String) {
        viewModelScope.launch { favoritesRepository.remove(path) }
    }

    class Factory(
        private val favoritesRepository: FavoritesRepository,
        private val fileRepository: FileRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FavoritesViewModel(favoritesRepository, fileRepository) as T
        }
    }
}
