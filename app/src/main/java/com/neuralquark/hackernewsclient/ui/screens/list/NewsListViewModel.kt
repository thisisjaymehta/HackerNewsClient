package com.neuralquark.hackernewsclient.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralquark.hackernewsclient.data.model.Story
import com.neuralquark.hackernewsclient.data.model.StoryCategory
import com.neuralquark.hackernewsclient.data.repository.HackerNewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedCategory: StoryCategory = StoryCategory.TOP,
    val newStoriesCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewsListViewModel @Inject constructor(
    private val repository: HackerNewsRepository
) : ViewModel() {
    
    private val _selectedCategory = MutableStateFlow(StoryCategory.TOP)
    private val _isLoading = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<NewsListUiState> = combine(
        _selectedCategory,
        _isLoading,
        _isRefreshing,
        _isLoadingMore,
        _error,
        repository.newStoriesAvailable
    ) { category, isLoading, isRefreshing, isLoadingMore, error, newStoriesMap ->
        NewsListUiState(
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            isLoadingMore = isLoadingMore,
            error = error,
            selectedCategory = category,
            newStoriesCount = newStoriesMap[category] ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewsListUiState(isLoading = true)
    )
    
    // Stories flow that automatically switches when category changes
    val stories: StateFlow<List<Story>> = _selectedCategory
        .flatMapLatest { category ->
            repository.getStoriesByCategory(category)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private var loadJob: Job? = null
    
    init {
        loadStoriesForCategory(StoryCategory.TOP)
    }
    
    fun selectCategory(category: StoryCategory) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            loadStoriesForCategory(category)
        }
    }
    
    private fun loadStoriesForCategory(category: StoryCategory) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Check if we have cached stories
            val hasCachedStories = repository.hasStoriesInCache(category)
            
            if (!hasCachedStories) {
                // No cache, fetch from network first
                val result = repository.refreshStories(category)
                result.onFailure { e ->
                    _error.value = e.message ?: "Failed to load stories"
                }
            } else {
                // Refresh in background
                refreshStoriesInBackground()
            }
            
            _isLoading.value = false
        }
    }
    
    fun refreshStories() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            
            val result = repository.refreshStories(_selectedCategory.value)
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to refresh stories"
            }
            
            _isRefreshing.value = false
            _isLoading.value = false
        }
    }
    
    private fun refreshStoriesInBackground() {
        viewModelScope.launch {
            repository.refreshStories(_selectedCategory.value)
        }
    }
    
    fun loadMore() {
        if (_isLoadingMore.value || _isLoading.value) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            
            val result = repository.loadMoreStories(_selectedCategory.value)
            result.onFailure { e ->
                _error.value = e.message ?: "Failed to load more stories"
            }
            
            _isLoadingMore.value = false
        }
    }
    
    fun onNewStoriesViewed() {
        repository.clearNewStoriesCount(_selectedCategory.value)
    }
    
    fun clearError() {
        _error.value = null
    }
}
