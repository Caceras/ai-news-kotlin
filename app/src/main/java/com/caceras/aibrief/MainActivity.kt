package com.caceras.aibrief

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caceras.aibrief.data.FeedFreshness
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.PostBlock
import com.caceras.aibrief.ui.components.ArticleArtwork
import com.caceras.aibrief.ui.components.FeatureStory
import com.caceras.aibrief.ui.components.PostBlockContent
import com.caceras.aibrief.ui.components.StoryRow
import com.caceras.aibrief.ui.components.TopicsStrip
import com.caceras.aibrief.ui.theme.AiBriefTheme
import com.caceras.aibrief.update.UpdateBanner
import com.caceras.aibrief.update.UpdateManifest
import com.caceras.aibrief.update.UpdateState
import com.caceras.aibrief.update.UpdateViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private const val SUPPORT_EMAIL = "rikicaceras@gmail.com"
private const val PRIVACY_URL = "https://github.com/Caceras/ai-news-kotlin/blob/main/PRIVACY.md"
private const val CONTACT_URL = "https://github.com/Caceras/ai-news-kotlin/blob/main/CONTACT.md"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiBriefTheme {
                val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModel.Factory)
                val updateViewModel: UpdateViewModel = viewModel()
                AiBriefApp(newsViewModel, updateViewModel)
            }
        }
    }
}

private enum class NavTab { HOME, SAVED, ABOUT }

@Composable
private fun AiBriefApp(viewModel: NewsViewModel, updateViewModel: UpdateViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var selectedArticleId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchShown by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // The open story stays on screen when it leaves both lists, such as
    // removing a saved story the current edition no longer carries, so the
    // reader can save it again instead of being thrown back to the feed.
    var lastShownArticle by remember { mutableStateOf<NewsArticle?>(null) }
    val listedArticle = (state.articles + state.savedArticles)
        .firstOrNull { it.id == selectedArticleId }
    val selectedArticle = listedArticle ?: lastShownArticle?.takeIf { it.id == selectedArticleId }
    SideEffect { if (listedArticle != null) lastShownArticle = listedArticle }
    val pagerState = rememberPagerState(initialPage = currentTab.ordinal,
        pageCount = { NavTab.entries.size })
    val scope = rememberCoroutineScope()
    val homeListState = rememberLazyListState()
    val savedListState = rememberLazyListState()
    val aboutListState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    fun selectTab(tab: NavTab) {
        keyboard?.hide()
        searchShown = false
        query = ""
        if (tab == NavTab.HOME && currentTab == NavTab.HOME) {
            scope.launch { homeListState.animateScrollToItem(0) }
            viewModel.refresh(userRequested = true)
            updateViewModel.checkForUpdate()
        }
        currentTab = tab
        scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
    }

    LaunchedEffect(Unit) {
        viewModel.notices.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    // A story restored after the app was reclaimed, or dropped by a newer
    // edition, must not reopen later out of nowhere once the device copy loads.
    LaunchedEffect(state.isLoading, selectedArticle) {
        if (!state.isLoading && selectedArticle == null) selectedArticleId = null
    }
    LaunchedEffect(pagerState.settledPage) { currentTab = NavTab.entries[pagerState.settledPage] }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        (updateState as? UpdateState.NeedsPermission)?.let { updateViewModel.retryInstall(it.apk) }
    }
    BackHandler(enabled = selectedArticle != null || searchShown || currentTab != NavTab.HOME) {
        when {
            selectedArticle != null -> selectedArticleId = null
            searchShown -> { searchShown = false; query = ""; keyboard?.hide() }
            else -> selectTab(NavTab.HOME)
        }
    }

    // The entire viewport, including the reader and fixed navigation, respects
    // system bars, cutouts and the keyboard rather than padding only list items.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().padding(WindowInsets.safeDrawing.asPaddingValues())) {
            AnimatedContent(targetState = selectedArticle, modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "articleTransition") { article ->
                if (article != null) {
                    ArticleDetailScreen(article, state.savedArticles.any { it.id == article.id },
                        onToggleSaved = {
                            val wasSaved = state.savedArticles.any { it.id == article.id }
                            viewModel.toggleSaved(article)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(if (wasSaved) "Removed from your reading list"
                                    else "Saved for offline reading", duration = SnackbarDuration.Short)
                            }
                        }, onBack = { selectedArticleId = null })
                } else {
                    Column(Modifier.fillMaxSize()) {
                        JournalMasthead(
                            isRefreshing = state.isRefreshing,
                            onHome = { selectTab(NavTab.HOME) },
                            onSearch = if (currentTab == NavTab.HOME) ({
                                searchShown = !searchShown
                                query = ""
                                if (searchShown) viewModel.selectCategory(NewsCategory.ALL)
                                else keyboard?.hide()
                                scope.launch { homeListState.scrollToItem(0) }
                            }) else null,
                            onRefresh = if (currentTab == NavTab.HOME) ({
                                viewModel.refresh(userRequested = true); updateViewModel.checkForUpdate()
                            }) else null,
                        )
                        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f),
                            beyondViewportPageCount = 1) { page ->
                            when (NavTab.entries[page]) {
                                NavTab.HOME -> FeedScreen(state, updateState, homeListState,
                                    searchShown, query, onQueryChange = { query = it },
                                    onCloseSearch = { query = ""; searchShown = false; keyboard?.hide() },
                                    onSelectCategory = viewModel::selectCategory,
                                    onOpenArticle = { keyboard?.hide(); selectedArticleId = it.id },
                                    onInstallUpdate = updateViewModel::downloadAndInstall,
                                    onGrantInstallPermission = updateViewModel::requestInstallPermission,
                                    onDismissUpdate = updateViewModel::dismiss,
                                    onAbout = { selectTab(NavTab.ABOUT) })
                                NavTab.SAVED -> SavedScreen(state.savedArticles, state.isLoading, savedListState,
                                    onOpenArticle = { selectedArticleId = it.id },
                                    onBrowse = { selectTab(NavTab.HOME) })
                                NavTab.ABOUT -> AboutScreen(aboutListState)
                            }
                        }
                        if (!keyboardVisible) JournalNavigation(currentTab, ::selectTab)
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp,
                    bottom = if (selectedArticle == null && !keyboardVisible) 78.dp else 16.dp))
        }
    }
}

@Composable
private fun JournalMasthead(
    isRefreshing: Boolean,
    onHome: () -> Unit,
    onSearch: (() -> Unit)?,
    onRefresh: (() -> Unit)?,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("AI Brief", style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 29.sp, lineHeight = 34.sp, letterSpacing = (-0.8).sp),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onHome))
                if (onSearch != null) IconButton(onClick = onSearch) {
                    Icon(Icons.Outlined.Search, "Search articles", Modifier.size(22.dp))
                }
                if (onRefresh != null) IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 1.5.dp)
                    else Icon(Icons.Outlined.Refresh, "Refresh edition", Modifier.size(22.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun JournalNavigation(activeTab: NavTab, onSelect: (NavTab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                NavTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    val color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(Modifier.weight(1f).heightIn(min = 64.dp)
                        .semantics { selected = isSelected }
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Icon(when (tab) {
                            NavTab.HOME -> Icons.Outlined.Newspaper
                            NavTab.SAVED -> if (isSelected) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder
                            NavTab.ABOUT -> Icons.Outlined.Info
                        }, null, Modifier.size(21.dp), tint = color)
                        Spacer(Modifier.height(4.dp))
                        Text(when (tab) { NavTab.HOME -> "Latest"; NavTab.SAVED -> "Saved"; NavTab.ABOUT -> "About" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedScreen(
    state: NewsUiState,
    updateState: UpdateState,
    listState: LazyListState,
    searchShown: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSelectCategory: (NewsCategory) -> Unit,
    onOpenArticle: (NewsArticle) -> Unit,
    onInstallUpdate: (UpdateManifest) -> Unit,
    onGrantInstallPermission: () -> Unit,
    onDismissUpdate: () -> Unit,
    onAbout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    // Refiltered only when the edition, topic or query changes, not on every
    // recomposition such as the refresh indicator toggling.
    val articles = remember(state.articles, state.selectedCategory, query) {
        val needle = query.trim()
        state.articles.filter {
            (state.selectedCategory == NewsCategory.ALL || it.category == state.selectedCategory) &&
                (needle.isEmpty() || it.searchableText.contains(needle, ignoreCase = true))
        }
    }
    val lead = articles.firstOrNull()
    LaunchedEffect(searchShown) { if (searchShown) focus.requestFocus() }

    Column(Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp)) {
        if (searchShown) {
            OutlinedTextField(query, onQueryChange,
                placeholder = { Text("Search the journal") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)) },
                trailingIcon = { IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Outlined.Close, "Close search", Modifier.size(20.dp))
                } },
                singleLine = true, shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                    .focusRequester(focus),
            )
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)) {
            item("update") {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    UpdateBanner(updateState, onInstallUpdate, onGrantInstallPermission, onDismissUpdate)
                }
            }
            if (!searchShown) item("edition") {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    val date = state.articles.maxOfOrNull { it.publishedAt }
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(date?.let { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)
                            .withZone(ZoneId.systemDefault()).format(it).uppercase(Locale.US) }
                            ?: "THE JOURNAL",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = .9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(4.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(Modifier.width(5.dp))
                        Text(when (state.freshness) {
                            FeedFreshness.LIVE -> "Latest edition"
                            FeedFreshness.CACHED -> "Cached edition"
                            FeedFreshness.OFFLINE -> "Offline edition"
                        }, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CategoryFilterRow(state.selectedCategory) { category ->
                        onSelectCategory(category)
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (searchShown) item("results-label") {
                Text("${articles.size} ${if (articles.size == 1) "story" else "stories"}" +
                    if (query.isBlank()) " in the journal" else " found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            if (lead != null) {
                if (!searchShown) {
                    item("lead-${lead.id}") {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            FeatureStory(lead) { onOpenArticle(lead) }
                        }
                    }
                    if (articles.size > 1) item("more-label") {
                        Text("More to explore", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 20.dp, top = 26.dp, bottom = 14.dp)
                                .semantics { heading() })
                    }
                }
                items(if (searchShown) articles else articles.drop(1), key = { it.id }) { article ->
                    Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)) {
                        StoryRow(article) { onOpenArticle(article) }
                    }
                }
            } else if (state.isLoading) item("loading") {
                LoadingIndicator()
            } else item("empty") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp)) {
                    Text(if (query.isBlank()) "More stories are on their way." else "No stories found.",
                        style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Try a different topic or search term.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.selectedCategory == NewsCategory.ALL && !searchShown) item("topics") {
                Spacer(Modifier.height(22.dp))
                TopicsStrip { category ->
                    onSelectCategory(category)
                    scope.launch { listState.animateScrollToItem(0) }
                }
            }
            item("editorial-note") {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text("A little context goes a long way.", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Original writing. Primary sources. Space to think.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Meet AI Brief  →", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.minimumInteractiveComponentSize()
                            .clickable(role = Role.Button, onClick = onAbout))
                }
            }
        }
    }
}

@Composable
private fun SavedScreen(
    articles: List<NewsArticle>,
    isLoading: Boolean,
    listState: LazyListState,
    onOpenArticle: (NewsArticle) -> Unit,
    onBrowse: () -> Unit,
) {
    LazyColumn(state = listState,
        modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 32.dp)) {
        item("header") {
            Text("Your reading list", style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text("Good reads, kept for later.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (articles.isNotEmpty()) Text(
                "${articles.size} ${if (articles.size == 1) "story" else "stories"} · available offline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp))
        }
        items(articles, key = { it.id }) { article ->
            StoryRow(article) { onOpenArticle(article) }
            Spacer(Modifier.height(10.dp))
        }
        if (isLoading) item("loading") {
            LoadingIndicator()
        } else if (articles.isEmpty()) item("empty") {
            Column(Modifier.fillMaxWidth().padding(vertical = 42.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(27.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(22.dp))
                Text("Keep a good read", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                Text("Save a story while you read.\nIt will be here, even when you’re offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(22.dp))
                Button(onClick = onBrowse, shape = RoundedCornerShape(4.dp)) { Text("Explore the latest") }
            }
        }
    }
}

/** Shown for the moment it takes to read this device's copy of the edition. */
@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp).semantics { contentDescription = "Loading stories" },
        contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun CategoryFilterRow(selectedCategory: NewsCategory, onSelectCategory: (NewsCategory) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        NewsCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 48.dp).heightIn(min = 48.dp)
                .semantics { selected = isSelected }
                .clickable(role = Role.Tab) {
                    if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectCategory(category)
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(category.label, style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent))
            }
        }
    }
}

@Composable
private fun ArticleDetailScreen(
    article: NewsArticle,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp)) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to stories", Modifier.size(22.dp))
                    }
                    Text("AI Brief", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).padding(start = 4.dp))
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSaved()
                    }) { Icon(if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        if (isSaved) "Remove from saved" else "Save article", Modifier.size(22.dp),
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = { context.shareArticle(article) }) {
                        Icon(Icons.Outlined.Share, "Share article", Modifier.size(21.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 40.dp)) {
            item("header") {
                SelectionContainer {
                    Column {
                        Text(article.category.label.uppercase(Locale.US) +
                            if (article.isOriginalPost) "  /  EXPLAINER" else "",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(article.title, style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { heading() })
                        if (article.isOriginalPost && article.summary.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(article.summary, style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(article.author?.let { "By $it" } ?: article.source,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("${formatArticleDate(article.publishedAt)} · ${article.readingTimeMinutes} min read" +
                            if (article.author != null) " · ${article.source}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (article.isOfflineBrief) Text("Offline explainer · background reading",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(24.dp))
            }
            if (article.isOriginalPost || !article.imageUrl.isNullOrBlank()) item("image") {
                ArticleArtwork(article, Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)), featured = true)
                if (article.imageUrl.isNullOrBlank()) Text("AI-generated editorial illustration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(26.dp))
            }
            item("prose") {
                SelectionContainer {
                    Column {
                        if (article.isOriginalPost) article.blocks.forEachIndexed { index, block ->
                            if (index > 0) Spacer(Modifier.height(20.dp))
                            PostBlockContent(block)
                        } else Text(article.summary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
            item("actions") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(20.dp))
                if (article.url.isNotBlank()) OutlinedButton(
                    onClick = { context.openExternalUrl(article.url) }, shape = RoundedCornerShape(4.dp)) {
                    Text(if (article.isOfflineBrief) "Open context" else "Read original source")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
                }
                Text(if (article.isOriginalPost) "Published by AI Brief · Caceras" else "Reporting and research from ${article.source}.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Text("Back to stories  ←", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onBack))
            }
        }
    }
}

@Composable
private fun AboutScreen(listState: LazyListState) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 24.dp,
            end = 20.dp,
            bottom = 44.dp,
        ),
    ) {
        item("header") {
            Text(
                text = "About AI Brief",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "AI Brief is an independent journal about artificial intelligence. Original editorial explainers and briefings are published by Caceras, with sources you can check.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.widthIn(max = 520.dp),
            )
            Spacer(Modifier.height(28.dp))
        }

        item("sources") {
            Text(
                text = "Our editorial approach",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Each story names its publisher, publication date and author when available. Original explainers include links to primary sources. The launch edition includes AI-assisted editorial writing; the app does not generate answers or images from reader prompts.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(32.dp))
        }

        item("privacy") {
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No accounts, advertising SDKs or analytics trackers. Saved articles and search queries stay on this device. The app fetches our public edition and any article images over HTTPS. Hosting providers receive normal connection metadata such as your IP address. Clear app storage or uninstall to delete saved articles and caches.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text("Read the privacy policy ↗", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.minimumInteractiveComponentSize()
                    .clickable(role = Role.Button) { context.openExternalUrl(PRIVACY_URL) })
            Spacer(Modifier.height(32.dp))
        }

        item("contact") {
            Text(
                text = "Contact & Corrections",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Questions, corrections, or source suggestions are welcome at $SUPPORT_EMAIL.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text("Publisher and contact details ↗", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.minimumInteractiveComponentSize()
                    .clickable(role = Role.Button) { context.openExternalUrl(CONTACT_URL) })
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            context.sendEmail(SUPPORT_EMAIL)
                        },
                    ),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
            ) {
                Text(
                    text = "Email the editor",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(40.dp))
        }

        item("version") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.BUILD_NUMBER})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Every date in the app reads the same way: "March 4, 2026". */
private val ARTICLE_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)

// The zone is resolved per call rather than cached with the pattern, so dates
// stay correct if the device changes time zone while the app is running.
private fun formatArticleDate(instant: Instant): String =
    ARTICLE_DATE_FORMAT.withZone(ZoneId.systemDefault()).format(instant)

/** Starts [intent], telling the reader when no installed app can handle it instead of failing silently. */
private fun Context.startOrExplain(intent: Intent, unavailable: String) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, unavailable, Toast.LENGTH_LONG).show()
    }
}

private fun Context.openExternalUrl(url: String) =
    startOrExplain(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser is available to open this link.")

private fun Context.shareArticle(article: NewsArticle) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, buildString {
            append(article.title)
            append("\n\n")
            append(article.summary)
            if (article.url.isNotBlank()) { append("\n\n"); append(article.url) }
            if (article.isOriginalPost) {
                article.blocks.filterIsInstance<PostBlock.Link>().forEach {
                    append("\n\n${it.text}: ${it.url}")
                }
                append("\n\nFrom AI Brief")
            }
        })
    }
    startOrExplain(Intent.createChooser(shareIntent, "Share article"), "No app is available for sharing.")
}

private fun Context.sendEmail(address: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "AI Brief feedback")
    }
    startOrExplain(intent, "No email app is set up. Write to $address.")
}
