package com.neuralquark.hackernewsclient.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralquark.hackernewsclient.data.model.Comment
import com.neuralquark.hackernewsclient.data.model.Story
import com.neuralquark.hackernewsclient.data.repository.HackerNewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryDetailUiState(
    val story: Story? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingComments: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HackerNewsRepository
) : ViewModel() {
    
    private val storyId: Long = savedStateHandle["storyId"] ?: 0
    
    private val _uiState = MutableStateFlow(StoryDetailUiState())
    val uiState: StateFlow<StoryDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadStory()
    }
    
    private fun loadStory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val story = repository.getStoryById(storyId)
                _uiState.value = _uiState.value.copy(
                    story = story,
                    isLoading = false
                )
                
                // Load comments
                loadComments()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load story"
                )
            }
        }
    }
    
    private fun loadComments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            
            // First load from cache
            repository.getCommentsForStory(storyId).collect { cachedComments ->
                _uiState.value = _uiState.value.copy(
                    comments = cachedComments,
                    isLoadingComments = false
                )
            }
        }
        
        // Refresh comments from network
        viewModelScope.launch {
            repository.refreshComments(storyId)
        }
    }
    
    fun refreshComments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            
            val result = repository.refreshComments(storyId)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to refresh comments"
                )
            }
            
            _uiState.value = _uiState.value.copy(isLoadingComments = false)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
