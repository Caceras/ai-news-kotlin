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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NewsUiState(
    val articles: List<NewsArticle>,
    val savedArticles: List<NewsArticle>,
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val isRefreshing: Boolean = false,
    /** True only until the copy stored on this device has been read. */
    val isLoading: Boolean = false,
    val freshness: FeedFreshness = FeedFreshness.OFFLINE,
    val lastUpdated: Instant? = null,
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NewsRepository(application)
    // Starts empty and refreshing. The device's copy is read from disk off the
    // main thread so a cold start never blocks the first frame on storage.
    private val _uiState = MutableStateFlow(
        NewsUiState(articles = emptyList(), savedArticles = emptyList(), isLoading = true, isRefreshing = true),
    )
    val uiState = _uiState.asStateFlow()

    /** One-off notices, such as a reader-requested refresh that could not reach the edition. */
    private val _notices = Channel<String>(Channel.CONFLATED)
    val notices = _notices.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                val (initial, saved) = withContext(Dispatchers.IO) {
                    repository.initialLoad() to repository.savedArticles()
                }
                _uiState.value = _uiState.value.copy(
                    articles = initial.articles,
                    savedArticles = saved,
                    freshness = initial.freshness,
                    lastUpdated = initial.updatedAt,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.value = _uiState.value.copy(articles = SeedArticles.create())
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            loadLatest(userRequested = false)
        }
    }

    /** [userRequested] refreshes report a failure; the silent launch refresh relies on the edition label. */
    fun refresh(userRequested: Boolean = false) {
        if (_uiState.value.isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch { loadLatest(userRequested) }
    }

    private suspend fun loadLatest(userRequested: Boolean) {
        try {
            val load = repository.loadLatest()
            _uiState.value = _uiState.value.copy(
                articles = load.articles,
                freshness = load.freshness,
                lastUpdated = load.updatedAt,
            )
            if (userRequested && load.freshness != FeedFreshness.LIVE) _notices.trySend(OFFLINE_NOTICE)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (userRequested) _notices.trySend(OFFLINE_NOTICE)
        } finally {
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun selectCategory(category: NewsCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun toggleSaved(article: NewsArticle) {
        _uiState.value = _uiState.value.copy(savedArticles = repository.toggleSaved(article))
    }

    private companion object {
        const val OFFLINE_NOTICE = "Couldn't reach the latest edition. Showing the copy on this device."
    }
}
