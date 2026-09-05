package com.naze.files.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naze.files.data.model.FileItem
import com.naze.files.data.recent.RecentFilesRepository
import com.naze.files.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecentEntryUi(val path: String, val item: FileItem?)

class RecentViewModel(
    private val recentFilesRepository: RecentFilesRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _entries = MutableStateFlow<List<RecentEntryUi>>(emptyList())
    val entries: StateFlow<List<RecentEntryUi>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            recentFilesRepository.recentEntries.collect { recent ->
                _isLoading.value = true
                _entries.value = recent.map { RecentEntryUi(it.path, fileRepository.stat(it.path)) }
                _isLoading.value = false
            }
        }
    }

    fun clear() {
        viewModelScope.launch { recentFilesRepository.clear() }
    }

    class Factory(
        private val recentFilesRepository: RecentFilesRepository,
        private val fileRepository: FileRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecentViewModel(recentFilesRepository, fileRepository) as T
        }
    }
}
