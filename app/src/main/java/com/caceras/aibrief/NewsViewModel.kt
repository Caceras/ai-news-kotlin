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
import com.caceras.aibrief.data.EditionExtras
import com.caceras.aibrief.data.FeedFreshness
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.NewsRepository
import com.caceras.aibrief.data.SeedArticles
import com.caceras.aibrief.data.restoredSaved
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
    /** Model rankings and the radar, read from the same edition document. */
    val extras: EditionExtras = EditionExtras.EMPTY,
    /** Stories this reader has opened, oldest first; see [ReadHistory]. */
    val readIds: Set<String> = emptySet(),
    /** How large story text is set in the reader; see [ReaderTextSize]. */
    val readerTextSize: ReaderTextSize = ReaderTextSize.STANDARD,
)

/** Reads a topic saved by [NewsViewModel.selectCategory], falling back to all topics. */
internal fun restoredCategory(saved: String?): NewsCategory =
    NewsCategory.entries.firstOrNull { it.name == saved } ?: NewsCategory.ALL

/**
 * The read list after the stored one ([stored]) arrives: stories opened while it
 * loaded ([openedWhileLoading]) are added, unless the reader cleared the history in
 * that moment, in which case the stored list is dropped too.
 */
internal fun mergedReadIds(stored: Set<String>, openedWhileLoading: Set<String>, cleared: Boolean): Set<String> =
    if (cleared) openedWhileLoading else openedWhileLoading.fold(stored) { ids, id -> ReadHistory.marked(ids, id) }

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
    /**
     * True when the reader asked for the refresh in flight, including a pull
     * that arrived while a silent resume refresh was already running: that
     * reader is waiting on the spinner and must hear if the edition was offline.
     */
    private var readerWaiting = false
    /** True once the reader has changed the text size in this session. */
    private var textSizeChosen = false
    /** True once the reader cleared the read list in this session. */
    private var historyCleared = false
    /**
     * True once the shelf and read list stored on this device have been read.
     * Until then nothing is written over them: a write in that moment would
     * replace the stored lists with only the stories touched since launch.
     */
    private var personalLoaded = false
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
            // The edition and the reader's own lists are read separately: a stored
            // edition that cannot be read must not leave the shelf looking empty, or
            // the next save would overwrite every saved story on disk.
            try {
                val initial = withContext(Dispatchers.IO) { repository.initialLoad() }
                _uiState.value = _uiState.value.copy(
                    articles = initial.articles,
                    freshness = initial.freshness,
                    lastUpdated = initial.updatedAt,
                    extras = initial.extras,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.value = _uiState.value.copy(articles = SeedArticles.create())
            }
            try {
                val (saved, read, storedSize) = withContext(Dispatchers.IO) {
                    Triple(repository.savedArticles(), repository.readStoryIds(), repository.readerTextSize())
                }
                // A size picked while the device copy was loading wins over the stored one.
                val textSize = if (textSizeChosen) _uiState.value.readerTextSize else storedSize
                _uiState.value = _uiState.value.copy(
                    savedArticles = saved,
                    // A story opened while the device copy was loading stays read,
                    // and a history cleared in that moment stays cleared.
                    readIds = mergedReadIds(read, _uiState.value.readIds, historyCleared),
                    readerTextSize = textSize,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
            } finally {
                personalLoaded = true
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            // markRead holds back writes while loading; save any change made in that moment.
            if (historyCleared || _uiState.value.readIds.isNotEmpty()) persistReadHistory()
            loadLatest()
        }
    }

    /** [userRequested] refreshes report a failure, even when they join one already running; the silent launch and resume refreshes rely on the edition label. */
    fun refresh(userRequested: Boolean = false) {
        if (userRequested) readerWaiting = true
        if (_uiState.value.isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch { loadLatest() }
    }

    /**
     * Called when the app returns to the foreground. A reader who leaves AI
     * Brief in the background overnight would otherwise keep seeing
     * yesterday's edition until they found the refresh button.
     */
    fun refreshIfStale() {
        if (isEditionStale(lastRefreshAttempt, SystemClock.elapsedRealtime())) refresh(userRequested = false)
    }

    private suspend fun loadLatest() {
        lastRefreshAttempt = SystemClock.elapsedRealtime()
        try {
            val load = repository.loadLatest()
            _uiState.value = _uiState.value.copy(
                articles = load.articles,
                freshness = load.freshness,
                lastUpdated = load.updatedAt,
                extras = load.extras,
            )
            if (readerWaiting && load.freshness != FeedFreshness.LIVE) _notices.trySend(OFFLINE_NOTICE)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (readerWaiting) _notices.trySend(OFFLINE_NOTICE)
        } finally {
            readerWaiting = false
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
        // Before the stored shelf is read, saving would replace it with this one story.
        if (!personalLoaded) return
        _uiState.update { it.copy(savedArticles = toggledSaved(it.savedArticles, article)) }
        persistShelf()
    }

    /** Undoes a removal: puts [article] back at [index], the place it had on the shelf. */
    fun restoreSaved(article: NewsArticle, index: Int) {
        if (!personalLoaded) return
        _uiState.update { it.copy(savedArticles = restoredSaved(it.savedArticles, article, index)) }
        persistShelf()
    }

    /** Remembers that [article] was opened; written on the IO dispatcher, like the shelf. */
    fun markRead(article: NewsArticle) {
        if (article.id in _uiState.value.readIds) return
        _uiState.update { it.copy(readIds = ReadHistory.marked(it.readIds, article.id)) }
        // Until the stored list is loaded, writing would replace it with this one story.
        if (!personalLoaded) return
        persistReadHistory()
    }

    /**
     * Forgets every opened story, so all headlines show as unread again.
     * Returns the forgotten list so the caller can offer Undo.
     */
    fun clearReadHistory(): Set<String> {
        val forgotten = _uiState.value.readIds
        historyCleared = true
        _uiState.update { it.copy(readIds = emptySet()) }
        if (personalLoaded) persistReadHistory()
        return forgotten
    }

    /** Undoes [clearReadHistory]: [forgotten] is read again, and so is every story opened since. */
    fun restoreReadHistory(forgotten: Set<String>) {
        // Before the stored list is read, undoing lets it merge back in when it arrives.
        if (!personalLoaded) historyCleared = false
        _uiState.update { it.copy(readIds = mergedReadIds(forgotten, it.readIds, cleared = false)) }
        if (personalLoaded) persistReadHistory()
    }

    /** Moves story text to the next size and says which; stored on the IO dispatcher. */
    fun cycleReaderTextSize() {
        textSizeChosen = true
        _uiState.update { it.copy(readerTextSize = it.readerTextSize.next()) }
        val size = _uiState.value.readerTextSize
        _notices.trySend("Text size: ${size.label}")
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { runCatching { repository.persistReaderTextSize(_uiState.value.readerTextSize) } }
        }
    }

    private fun persistReadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { runCatching { repository.persistReadStoryIds(_uiState.value.readIds) } }
        }
    }

    private fun persistShelf() {
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
