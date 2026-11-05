package com.caceras.aibrief

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.caceras.aibrief.data.FeedFreshness
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.NewsRepository
import com.caceras.aibrief.data.SeedArticles
import com.caceras.aibrief.data.toggledSaved
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

/** Reads a topic saved by [NewsViewModel.selectCategory], falling back to all topics. */
internal fun restoredCategory(saved: String?): NewsCategory =
    NewsCategory.entries.firstOrNull { it.name == saved } ?: NewsCategory.ALL

/** How long the edition on screen is trusted before returning to the app checks for a newer one. */
internal const val STALE_AFTER_MILLIS = 30L * 60L * 1000L

/**
 * True when the last refresh started at least [STALE_AFTER_MILLIS] before [now].
 * Both values come from the same monotonic clock, so changing the device time
 * cannot suppress or force a refresh. Before the first attempt nothing is stale:
 * the launch refresh already covers it.
 */
internal fun isEditionStale(lastAttempt: Long?, now: Long): Boolean =
    lastAttempt != null && now - lastAttempt >= STALE_AFTER_MILLIS

class NewsViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = NewsRepository(application)
    private val saveLock = Mutex()
    /** Monotonic time of the latest refresh attempt, or null before the first one starts. */
    private var lastRefreshAttempt: Long? = null
    // Starts empty and refreshing. The device's copy is read from disk off the
    // main thread so a cold start never blocks the first frame on storage.
    // The chosen topic is restored after the system reclaims the app in the
    // background, so returning to it does not silently reset the filter.
    private val _uiState = MutableStateFlow(
        NewsUiState(articles = emptyList(), savedArticles = emptyList(), isLoading = true, isRefreshing = true,
            selectedCategory = restoredCategory(savedState[TOPIC_KEY])),
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

    /**
     * Called when the app returns to the foreground. A reader who leaves AI
     * Brief in the background overnight would otherwise keep seeing
     * yesterday's edition until they found the refresh button.
     */
    fun refreshIfStale() {
        if (isEditionStale(lastRefreshAttempt, SystemClock.elapsedRealtime())) refresh(userRequested = false)
    }

    private suspend fun loadLatest(userRequested: Boolean) {
        lastRefreshAttempt = SystemClock.elapsedRealtime()
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
        savedState[TOPIC_KEY] = category.name
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    /**
     * Updates the shelf in memory at once and writes it to disk on the IO
     * dispatcher: encoding up to 80 saved posts on the main thread made the
     * save button stutter. Each write stores the latest shelf under a lock, so
     * quick repeated taps can never leave an older shelf on disk.
     */
    fun toggleSaved(article: NewsArticle) {
        _uiState.update { it.copy(savedArticles = toggledSaved(it.savedArticles, article)) }
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock {
                runCatching { repository.persistSaved(_uiState.value.savedArticles) }
            }
        }
    }

    companion object {
        /** Builds the view model without reflection, so R8 needs no keep rule for its constructor. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NewsViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!,
                    createSavedStateHandle())
            }
        }
        private const val TOPIC_KEY = "topic"
        private const val OFFLINE_NOTICE = "Couldn't reach the latest edition. Showing the copy on this device."
    }
}
