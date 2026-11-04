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

    /** One-off notices, such as a reader-requested refresh that could not reach the edition. */
    private val _notices = Channel<String>(Channel.CONFLATED)
    val notices = _notices.receiveAsFlow()

    init {
        refresh()
    }

    /** [userRequested] refreshes report a failure; the silent launch refresh relies on the edition label. */
    fun refresh(userRequested: Boolean = false) {
        if (_uiState.value.isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch {
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
