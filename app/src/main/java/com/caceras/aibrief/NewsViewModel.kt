package com.caceras.aibrief

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.caceras.aibrief.data.FeedFreshness
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.NewsRepository
import com.caceras.aibrief.data.SeedArticles
import java.time.Instant
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NewsUiState(
    val articles: List<NewsArticle>,
    val savedArticles: List<NewsArticle>,
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val isRefreshing: Boolean = false,
    val freshness: FeedFreshness = FeedFreshness.OFFLINE,
    val lastUpdated: Instant? = null,
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NewsRepository(application)
    private val _uiState = MutableStateFlow(
        run {
            val initial = repository.initialLoad()
            NewsUiState(
                articles = initial.articles,
                savedArticles = repository.savedArticles(),
                freshness = initial.freshness,
                lastUpdated = initial.updatedAt,
            )
        },
    )
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val load = repository.loadLatest()
            _uiState.value = _uiState.value.copy(
                articles = load.articles,
                isRefreshing = false,
                freshness = load.freshness,
                lastUpdated = load.updatedAt,
            )
        }
    }

    fun selectCategory(category: NewsCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun toggleSaved(article: NewsArticle) {
        _uiState.value = _uiState.value.copy(savedArticles = repository.toggleSaved(article))
    }
}
