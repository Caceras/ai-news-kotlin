package com.caceras.aibrief

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.PostBlock
import com.caceras.aibrief.ui.components.ArticleImageCache
import com.caceras.aibrief.ui.components.ArticleReader
import com.caceras.aibrief.ui.components.BrandMark
import com.caceras.aibrief.ui.components.DesignedCard
import com.caceras.aibrief.ui.components.HeadlineCard
import com.caceras.aibrief.ui.components.LeadStory
import com.caceras.aibrief.ui.components.OpinionRail
import com.caceras.aibrief.ui.components.PairCards
import com.caceras.aibrief.ui.components.RowCard
import com.caceras.aibrief.ui.components.Rule
import com.caceras.aibrief.ui.components.SectionRibbon
import com.caceras.aibrief.ui.components.spokenAs
import com.caceras.aibrief.ui.theme.AiBriefTheme
import com.caceras.aibrief.ui.theme.Brief
import com.caceras.aibrief.ui.theme.BriefShapes
import com.caceras.aibrief.ui.theme.Space
import com.caceras.aibrief.update.UpdateBanner
import com.caceras.aibrief.update.UpdateManifest
import com.caceras.aibrief.update.UpdateState
import com.caceras.aibrief.update.UpdateViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SUPPORT_EMAIL = "rikicaceras@gmail.com"
private const val PRIVACY_URL = "https://github.com/Caceras/ai-news-kotlin/blob/main/PRIVACY.md"
private const val CONTACT_URL = "https://github.com/Caceras/ai-news-kotlin/blob/main/CONTACT.md"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The masthead is dark in both themes, so the status bar always uses light icons.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            AiBriefTheme {
                val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModel.Factory)
                val updateViewModel: UpdateViewModel = viewModel()
                AiBriefApp(newsViewModel, updateViewModel)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ArticleImageCache.onTrimMemory(level)
    }
}

private enum class NavTab(val label: String) { HOME("Latest"), SAVED("Saved"), ABOUT("About") }

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
    val context = LocalContext.current

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
        viewModel.refreshIfStale()
        (updateState as? UpdateState.NeedsPermission)?.let { updateViewModel.retryInstall(it.apk) }
    }
    BackHandler(enabled = selectedArticle != null || searchShown || currentTab != NavTab.HOME) {
        when {
            selectedArticle != null -> selectedArticleId = null
            searchShown -> { searchShown = false; query = ""; keyboard?.hide() }
            else -> selectTab(NavTab.HOME)
        }
    }

    val c = Brief.colors
    // The masthead colour runs up under the status bar; everything else
    // respects system bars, cutouts and the keyboard.
    Box(Modifier.fillMaxSize().background(c.masthead)
        .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())) {
        Box(Modifier.fillMaxSize().background(c.paper)
            .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues())) {
            AnimatedContent(targetState = selectedArticle, modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "articleTransition") { article ->
                if (article != null) {
                    ArticleReader(article, state.savedArticles.any { it.id == article.id },
                        onToggleSaved = {
                            val wasSaved = state.savedArticles.any { it.id == article.id }
                            viewModel.toggleSaved(article)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(if (wasSaved) "Removed from your reading list"
                                    else "Saved for offline reading", duration = SnackbarDuration.Short)
                            }
                        },
                        onShare = { context.shareArticle(article) },
                        onOpenSource = { context.openExternalUrl(article.url) },
                        onBack = { selectedArticleId = null })
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Masthead(
                            isRefreshing = state.isRefreshing,
                            showTopics = currentTab == NavTab.HOME && !searchShown,
                            selectedCategory = state.selectedCategory,
                            onSelectCategory = { category ->
                                viewModel.selectCategory(category)
                                scope.launch { homeListState.scrollToItem(0) }
                            },
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
                        if (!keyboardVisible) BottomNavigation(currentTab, ::selectTab)
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)
                .padding(start = Space.gutter, end = Space.gutter,
                    bottom = if (selectedArticle == null && !keyboardVisible) 78.dp else Space.gutter))
        }
    }
}

/** The navy bar: wordmark, search, refresh, and the topic tabs on the front page. */
@Composable
private fun Masthead(
    isRefreshing: Boolean,
    showTopics: Boolean,
    selectedCategory: NewsCategory,
    onSelectCategory: (NewsCategory) -> Unit,
    onHome: () -> Unit,
    onSearch: (() -> Unit)?,
    onRefresh: (() -> Unit)?,
) {
    val c = Brief.colors
    Column(Modifier.fillMaxWidth().background(c.masthead)) {
        Row(Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.xs),
            verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).minimumInteractiveComponentSize()
                .clickable(role = Role.Button, onClickLabel = "Go to latest stories", onClick = onHome),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 22.dp, height = 18.dp).background(c.ribbonLabel, BriefShapes.thumb))
                Spacer(Modifier.width(Space.s))
                Text("AI Brief", style = Brief.type.wordmark, color = c.onMasthead)
            }
            if (onSearch != null) IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, "Search articles", tint = c.onMasthead)
            }
            if (onRefresh != null) IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) CircularProgressIndicator(Modifier.size(18.dp), color = c.onMasthead,
                    strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, "Refresh edition", tint = c.onMasthead)
            }
        }
        if (showTopics) TopicTabs(selectedCategory, onSelectCategory)
    }
}

@Composable
private fun TopicTabs(selected: NewsCategory, onSelect: (NewsCategory) -> Unit) {
    val c = Brief.colors
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Space.s)) {
        NewsCategory.entries.forEach { category ->
            val isSelected = category == selected
            Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 48.dp).heightIn(min = 48.dp)
                .semantics { this.selected = isSelected }
                .clickable(role = Role.Tab) {
                    if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(category)
                }.padding(horizontal = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(category.label.uppercase(Locale.US), style = Brief.type.tag,
                    color = if (isSelected) c.onMasthead else c.onMasthead.copy(alpha = 0.66f),
                    modifier = Modifier.padding(top = Space.m, bottom = Space.m))
                Box(Modifier.fillMaxWidth().height(3.dp)
                    .background(if (isSelected) c.ribbonLabel else c.masthead))
            }
        }
    }
}

@Composable
private fun BottomNavigation(activeTab: NavTab, onSelect: (NavTab) -> Unit) {
    val c = Brief.colors
    Column(Modifier.background(c.card)) {
        Rule()
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            NavTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                val color = if (isSelected) c.accent else c.inkMuted
                Column(Modifier.weight(1f).heightIn(min = 64.dp)
                    .semantics { selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .padding(vertical = Space.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Icon(when (tab) {
                        NavTab.HOME -> Icons.Outlined.Newspaper
                        NavTab.SAVED -> if (isSelected) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder
                        NavTab.ABOUT -> Icons.Outlined.Info
                    }, null, Modifier.size(22.dp), tint = color)
                    Spacer(Modifier.height(Space.xs))
                    Text(tab.label, style = Brief.type.tag, color = color)
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
    onOpenArticle: (NewsArticle) -> Unit,
    onInstallUpdate: (UpdateManifest) -> Unit,
    onGrantInstallPermission: () -> Unit,
    onDismissUpdate: () -> Unit,
    onAbout: () -> Unit,
) {
    val c = Brief.colors
    val focus = remember { FocusRequester() }
    // Ticks once a minute while the app is in front, so "Updated 5 min ago"
    // never goes stale on screen, and stops in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    val now by produceState(Instant.now(), lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = Instant.now()
                delay(60_000)
            }
        }
    }
    // Refiltered only when the edition, topic or query changes, not on every
    // recomposition such as the refresh indicator toggling.
    val articles = remember(state.articles, state.selectedCategory, query) {
        val needle = query.trim()
        state.articles.filter {
            (state.selectedCategory == NewsCategory.ALL || it.category == state.selectedCategory) &&
                (needle.isEmpty() || it.searchableText.contains(needle, ignoreCase = true))
        }
    }
    // The front page is recomposed into slots only when the stories change.
    // Breaking news leaves the lead after a day, so the hour is part of the key.
    val hour = now.epochSecond / 3600
    val slots = remember(articles, hour) { FeedLayout.compose(articles, now) }
    LaunchedEffect(searchShown) { if (searchShown) focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(c.paper).wrapContentWidth().widthIn(max = 840.dp)) {
        if (searchShown) {
            OutlinedTextField(query, onQueryChange,
                placeholder = { Text("Search AI Brief", style = Brief.type.deck) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)) },
                trailingIcon = { IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Outlined.Close, "Close search", Modifier.size(20.dp))
                } },
                singleLine = true, shape = BriefShapes.button,
                textStyle = Brief.type.deck,
                modifier = Modifier.fillMaxWidth().background(c.card)
                    .padding(horizontal = Space.gutter, vertical = Space.m)
                    .focusRequester(focus),
            )
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Space.xl)) {
            item("update") {
                Box(Modifier.padding(horizontal = Space.gutter)) {
                    UpdateBanner(updateState, onInstallUpdate, onGrantInstallPermission, onDismissUpdate)
                }
            }
            if (!searchShown) item("edition") {
                val date = state.articles.maxOfOrNull { it.publishedAt }
                Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(date?.let { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.US)
                        .withZone(ZoneId.systemDefault()).format(it).uppercase(Locale.US) } ?: "AI BRIEF",
                        style = Brief.type.tag, color = c.inkMuted, modifier = Modifier.weight(1f))
                    Text(editionLabel(state.freshness, state.lastUpdated, now), style = Brief.type.meta,
                        color = c.inkMuted)
                }
            }
            if (searchShown) {
                item("results-label") {
                    Text("${articles.size} ${if (articles.size == 1) "story" else "stories"}" +
                        if (query.isBlank()) " in AI Brief" else " found",
                        style = Brief.type.meta, color = c.inkMuted,
                        // TalkBack announces the new count as the reader types.
                        modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.s)
                            .semantics { liveRegion = LiveRegionMode.Polite })
                }
                items(articles, key = { "result-${it.id}" }) { article ->
                    RowCard(article) { onOpenArticle(article) }
                }
            } else {
                items(slots, key = { it.key }, contentType = { it::class }) { slot ->
                    when (slot) {
                        is FeedSlot.Lead -> LeadStory(slot.article) { onOpenArticle(slot.article) }
                        is FeedSlot.Ribbon -> SectionRibbon(slot.title, breaking = slot.breaking)
                        is FeedSlot.Headline -> Box(Modifier.padding(top = Space.s)) {
                            HeadlineCard(slot.article) { onOpenArticle(slot.article) }
                        }
                        is FeedSlot.Pair -> Box(Modifier.padding(top = Space.s)) {
                            PairCards(slot.left, slot.right, onOpenArticle)
                        }
                        is FeedSlot.Row -> RowCard(slot.article) { onOpenArticle(slot.article) }
                        is FeedSlot.OpinionRail -> OpinionRail(slot.articles, onOpenArticle)
                        is FeedSlot.Designed -> DesignedCard(slot.article, Modifier.padding(Space.gutter)) {
                            onOpenArticle(slot.article)
                        }
                    }
                }
            }
            if (articles.isEmpty()) {
                if (state.isLoading) item("loading") { LoadingIndicator() }
                else item("empty") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = Space.xl, vertical = Space.section)) {
                        Text(if (query.isBlank()) "More stories are on their way." else "No stories found.",
                            style = Brief.type.headline, color = c.ink)
                        Spacer(Modifier.height(Space.m))
                        Text("Try a different topic or search term.", style = Brief.type.deck, color = c.inkMuted)
                    }
                }
            }
            if (!searchShown) item("editorial-note") {
                Column(Modifier.fillMaxWidth().padding(top = Space.xl).background(c.masthead)
                    .padding(horizontal = Space.gutter, vertical = Space.xl)) {
                    BrandMark(c.onMasthead)
                    Spacer(Modifier.height(Space.m))
                    Text("Original writing. Primary sources. No noise.", style = Brief.type.headlineSmall,
                        color = c.onMasthead)
                    Text("Meet AI Brief  →", style = Brief.type.button, color = c.onMasthead,
                        modifier = Modifier.minimumInteractiveComponentSize().spokenAs("Meet AI Brief")
                            .clickable(role = Role.Button, onClick = onAbout))
                }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    val c = Brief.colors
    Column(Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, top = Space.xl,
        bottom = Space.gutter)) {
        Text(title, style = Brief.type.pageTitle, color = c.ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        Text(subtitle, style = Brief.type.deck, color = c.inkMuted)
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
    val c = Brief.colors
    LazyColumn(state = listState,
        modifier = Modifier.fillMaxSize().background(c.paper).wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(bottom = Space.xxl)) {
        item("header") {
            PageHeader("Saved", "Good reads, kept for later.")
            if (articles.isNotEmpty()) SectionRibbon(
                "${articles.size} ${if (articles.size == 1) "story" else "stories"} offline")
        }
        items(articles, key = { it.id }) { article -> RowCard(article) { onOpenArticle(article) } }
        if (isLoading) item("loading") {
            LoadingIndicator()
        } else if (articles.isEmpty()) item("empty") {
            Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.section),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).background(c.masthead, BriefShapes.round),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(28.dp), tint = c.onMasthead)
                }
                Spacer(Modifier.height(Space.l))
                Text("Keep a good read", style = Brief.type.headline, color = c.ink)
                Spacer(Modifier.height(Space.s))
                Text("Save a story while you read.\nIt will be here, even when you’re offline.",
                    style = Brief.type.deck, textAlign = TextAlign.Center, color = c.inkMuted)
                Spacer(Modifier.height(Space.l))
                PrimaryButton("Explore the latest", onBrowse)
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    val c = Brief.colors
    Text(label, style = Brief.type.button, color = c.onAccent,
        modifier = Modifier.minimumInteractiveComponentSize().background(c.accent, BriefShapes.button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.l, vertical = Space.m))
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    val c = Brief.colors
    Text(label, style = Brief.type.button, color = c.ink,
        modifier = Modifier.minimumInteractiveComponentSize().border(Space.hairline, c.ink, BriefShapes.button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.l, vertical = Space.m))
}

/** Shown for the moment it takes to read this device's copy of the edition. */
@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxWidth().padding(vertical = Space.section)
        .semantics { contentDescription = "Loading stories" }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), color = Brief.colors.accent, strokeWidth = 2.dp)
    }
}

@Composable
private fun AboutSection(title: String, body: String, link: Pair<String, () -> Unit>? = null) {
    val c = Brief.colors
    Column(Modifier.fillMaxWidth().background(c.card).padding(Space.gutter)) {
        Text(title, style = Brief.type.bodyHeading, color = c.ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        Text(body, style = Brief.type.body, color = c.ink)
        link?.let { (label, open) ->
            Text("$label ↗", style = Brief.type.button, color = c.accent,
                modifier = Modifier.padding(top = Space.s).minimumInteractiveComponentSize().spokenAs(label)
                    .clickable(role = Role.Button, onClickLabel = "Open in browser", onClick = open))
        }
    }
    Spacer(Modifier.height(Space.s))
}

@Composable
private fun AboutScreen(listState: LazyListState) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val c = Brief.colors
    LazyColumn(state = listState,
        modifier = Modifier.fillMaxSize().background(c.paper).wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(bottom = Space.section)) {
        item("header") {
            PageHeader("About AI Brief",
                "An independent news journal about artificial intelligence, published by Caceras.")
        }
        item("approach") {
            SectionRibbon("Our standards")
            AboutSection("How we publish",
                "Every story names its author, publication date and topic, and links to primary sources you can " +
                    "check. Updates to a published story are dated. Columns are labelled Opinion and carry the " +
                    "author's name. Paid content is always labelled Sponsored with the sponsor's name, and never " +
                    "leads the front page.")
            AboutSection("AI and images",
                "Some writing is AI-assisted and edited by a person before publication. Illustrations made with AI " +
                    "are labelled. The app does not generate answers or images from your prompts.")
        }
        item("privacy") {
            SectionRibbon("Privacy")
            AboutSection("No accounts, no tracking",
                "No accounts, advertising SDKs or analytics trackers. Saved articles and search queries stay on this " +
                    "device. The app fetches our public edition and article images over HTTPS, so hosting providers " +
                    "receive normal connection data such as your IP address. Clear app storage or uninstall to " +
                    "delete saved articles and caches.",
                "Read the privacy policy" to { context.openExternalUrl(PRIVACY_URL) })
        }
        item("contact") {
            SectionRibbon("Contact")
            AboutSection("Contact and corrections",
                "Questions, corrections or source suggestions are welcome at $SUPPORT_EMAIL.",
                "Publisher and contact details" to { context.openExternalUrl(CONTACT_URL) })
            Box(Modifier.padding(horizontal = Space.gutter, vertical = Space.s)) {
                SecondaryButton("Email the editor") {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    context.sendEmail(SUPPORT_EMAIL)
                }
            }
        }
        item("version") {
            Text("Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.BUILD_NUMBER})",
                style = Brief.type.meta, color = c.inkMuted,
                modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.xl))
        }
    }
}

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
