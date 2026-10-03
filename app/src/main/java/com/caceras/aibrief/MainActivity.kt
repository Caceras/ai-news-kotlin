package com.caceras.aibrief

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.sp
import com.caceras.aibrief.data.FeedFreshness
import com.caceras.aibrief.data.PostBlock
import com.caceras.aibrief.ui.components.FeatureStory
import com.caceras.aibrief.ui.components.StoryRow
import com.caceras.aibrief.ui.components.TopicsStrip
import com.caceras.aibrief.ui.components.ArticleArtwork
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.ui.components.AsyncArticleImage
import com.caceras.aibrief.ui.components.PostBlockContent
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
private const val PRIVACY_URL = "https://caceras.github.io/ai-news-kotlin/privacy.html"
private const val CONTACT_URL = "https://caceras.github.io/ai-news-kotlin/contact.html"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiBriefTheme {
                val newsViewModel: NewsViewModel = viewModel()
                val updateViewModel: UpdateViewModel = viewModel()
                AiBriefApp(newsViewModel, updateViewModel)
            }
        }
    }
}

private enum class NavTab {
    HOME,
    SAVED,
    ABOUT,
}

@Composable
private fun AiBriefApp(viewModel: NewsViewModel, updateViewModel: UpdateViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var selectedArticleId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedArticle = (state.articles + state.savedArticles)
        .firstOrNull { it.id == selectedArticleId }
    val pagerState = rememberPagerState(
        initialPage = currentTab.ordinal,
        pageCount = { NavTab.entries.size },
    )
    val scope = rememberCoroutineScope()
    val homeListState = rememberLazyListState()
    val savedListState = rememberLazyListState()
    val aboutListState = rememberLazyListState()

    fun selectTab(tab: NavTab) {
        if (tab == NavTab.HOME && currentTab == NavTab.HOME) {
            viewModel.refresh()
            updateViewModel.checkForUpdate()
        }
        currentTab = tab
        scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
    }

    LaunchedEffect(pagerState.settledPage) {
        currentTab = NavTab.entries[pagerState.settledPage]
    }

    // Returning from the system settings screen is the moment a pending install
    // becomes possible, so the update finishes without another tap.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        (updateState as? UpdateState.NeedsPermission)?.let { updateViewModel.retryInstall(it.apk) }
    }

    BackHandler(enabled = selectedArticle != null || currentTab != NavTab.HOME) {
        if (selectedArticle != null) {
            selectedArticleId = null
        } else {
            currentTab = NavTab.HOME
            scope.launch { pagerState.animateScrollToPage(NavTab.HOME.ordinal) }
        }
    }

    // Keep the scrolling viewport inside system bars and display cutouts.
    // Padding only the list's first/last items lets scrolled text cross them.
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val topPadding = 0.dp
    val bottomPadding = 0.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = selectedArticle,
            modifier = Modifier.fillMaxSize().padding(safePadding),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "articleTransition",
        ) { article ->
            if (article != null) {
                ArticleDetailScreen(
                    article = article,
                    isSaved = state.savedArticles.any { it.id == article.id },
                    topPadding = topPadding,
                    bottomPadding = bottomPadding,
                    onToggleSaved = { viewModel.toggleSaved(article) },
                    onBack = { selectedArticleId = null },
                    onNavigateTab = {
                        selectedArticleId = null
                        selectTab(it)
                    },
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (NavTab.entries[page]) {
                        NavTab.HOME -> FeedScreen(
                            state = state,
                            updateState = updateState,
                            currentTab = currentTab,
                            listState = homeListState,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding,
                            onTabSelected = ::selectTab,
                            onSelectCategory = viewModel::selectCategory,
                            onOpenArticle = { selectedArticleId = it.id },
                            onInstallUpdate = updateViewModel::downloadAndInstall,
                            onGrantInstallPermission = updateViewModel::requestInstallPermission,
                            onDismissUpdate = updateViewModel::dismiss,
                            onRefresh = {
                                viewModel.refresh()
                                updateViewModel.checkForUpdate()
                            },
                        )
                        NavTab.SAVED -> SavedScreen(
                            articles = state.savedArticles,
                            currentTab = currentTab,
                            listState = savedListState,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding,
                            onTabSelected = ::selectTab,
                            onOpenArticle = { selectedArticleId = it.id },
                        )
                        NavTab.ABOUT -> AboutScreen(
                            currentTab = currentTab,
                            listState = aboutListState,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding,
                            onTabSelected = ::selectTab,
                        )
                    }
                }
            }
        }
    }
}

/** A magazine masthead, with explicit navigation and accessible actions. */
@Composable
private fun MinimalTopNav(
    activeTab: NavTab?,
    isRefreshing: Boolean = false,
    onTabSelected: (NavTab) -> Unit,
    onSearch: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("AI Brief", style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) { onTabSelected(NavTab.HOME) })
                Text("THE INDEPENDENT AI JOURNAL", style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp, letterSpacing = 1.4.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onSearch != null) IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, contentDescription = "Search articles")
            }
            if (onRefresh != null) IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, contentDescription = "Refresh edition")
            }
            IconButton(onClick = { onTabSelected(NavTab.ABOUT) }) {
                Icon(Icons.Outlined.Menu, contentDescription = "About, contact and privacy")
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            NavTextLink("Latest", activeTab == NavTab.HOME) { onTabSelected(NavTab.HOME) }
            NavTextLink("Saved", activeTab == NavTab.SAVED) { onTabSelected(NavTab.SAVED) }
            NavTextLink("About", activeTab == NavTab.ABOUT) { onTabSelected(NavTab.ABOUT) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NavTextLink(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun FeedScreen(
    state: NewsUiState,
    updateState: UpdateState,
    currentTab: NavTab,
    listState: LazyListState,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (NavTab) -> Unit,
    onSelectCategory: (NewsCategory) -> Unit,
    onOpenArticle: (NewsArticle) -> Unit,
    onInstallUpdate: (UpdateManifest) -> Unit,
    onGrantInstallPermission: () -> Unit,
    onDismissUpdate: () -> Unit,
    onRefresh: () -> Unit,
) {
    var searchShown by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val articles = state.articles.filter {
        (state.selectedCategory == NewsCategory.ALL || it.category == state.selectedCategory) &&
            (query.isBlank() || it.searchableText.contains(query.trim(), ignoreCase = true))
    }
    val lead = articles.firstOrNull()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(start = 20.dp, top = topPadding + 12.dp,
            end = 20.dp, bottom = bottomPadding + 32.dp),
    ) {
        item("nav") {
            MinimalTopNav(currentTab, state.isRefreshing, onTabSelected,
                onSearch = { searchShown = !searchShown; if (!searchShown) query = "" },
                onRefresh = onRefresh)
            Spacer(Modifier.height(24.dp))
        }
        item("update") {
            UpdateBanner(updateState, onInstallUpdate, onGrantInstallPermission, onDismissUpdate)
        }
        item("header") {
            Text("A clearer view of AI.", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text("Original briefings, useful context, and sources you can check.",
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            val freshness = when (state.freshness) {
                FeedFreshness.LIVE -> "Latest available edition"
                FeedFreshness.CACHED -> "Saved edition · refresh to check for new writing"
                FeedFreshness.OFFLINE -> "Offline edition · refresh when connected"
            }
            Text(freshness, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            if (searchShown) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Search articles") }, singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { query = ""; searchShown = false }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close search")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            CategoryFilterRow(state.selectedCategory, onSelectCategory)
            Spacer(Modifier.height(20.dp))
        }
        if (lead != null) {
            item("lead-${lead.id}") {
                FeatureStory(lead) { onOpenArticle(lead) }
                Spacer(Modifier.height(16.dp))
            }
            items(articles.drop(1), key = { it.id }) { article ->
                StoryRow(article) { onOpenArticle(article) }
                Spacer(Modifier.height(12.dp))
            }
        } else item("empty") {
            Text(if (query.isBlank()) "No stories in this topic yet." else "No stories match “$query”.",
                style = MaterialTheme.typography.titleLarge)
            Text("Try another topic or clear the search.", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp))
        }
        if (state.selectedCategory == NewsCategory.ALL && query.isBlank()) item("topics") {
            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(28.dp))
            TopicsStrip { category ->
                onSelectCategory(category)
                scope.launch { listState.animateScrollToItem(0) }
            }
        }
        item("editorial-note") {
            Spacer(Modifier.height(36.dp))
            Column(Modifier.fillMaxWidth().background(Color(0xFF082638)).padding(24.dp)) {
                Text("Independent. Curious. Useful.", style = MaterialTheme.typography.titleLarge,
                    color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("Editor-published writing about artificial intelligence. Read, check the sources, and keep what matters on your saved shelf.",
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD9E5E8))
                Spacer(Modifier.height(14.dp))
                Text("CONTACT THE EDITOR  →", style = MaterialTheme.typography.labelSmall,
                    color = Color.White, modifier = Modifier.minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) { onTabSelected(NavTab.ABOUT) })
            }
        }
    }
}

@Composable
private fun SavedScreen(
    articles: List<NewsArticle>,
    currentTab: NavTab,
    listState: LazyListState,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (NavTab) -> Unit,
    onOpenArticle: (NewsArticle) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = topPadding + 12.dp,
            end = 20.dp,
            bottom = bottomPadding + 44.dp,
        ),
    ) {
        item("nav") {
            MinimalTopNav(
                activeTab = currentTab,
                onTabSelected = onTabSelected,
            )
            Spacer(Modifier.height(28.dp))
        }

        item("header") {
            Text(
                text = "Saved",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Reads preserved locally on this device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(36.dp))
        }

        if (articles.isNotEmpty()) {
            items(articles, key = { it.id }) { article ->
                EditorialArticleRow(
                    article = article,
                    onClick = { onOpenArticle(article) },
                )
                Spacer(Modifier.height(22.dp))
            }
        } else {
            item("empty") {
                Text(
                    text = "Nothing saved yet. Tap any article to read and save it for offline access.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
            }
        }
    }
}

@Composable
private fun AboutScreen(
    currentTab: NavTab,
    listState: LazyListState,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (NavTab) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = topPadding + 12.dp,
            end = 20.dp,
            bottom = bottomPadding + 44.dp,
        ),
    ) {
        item("nav") {
            MinimalTopNav(
                activeTab = currentTab,
                onTabSelected = onTabSelected,
            )
            Spacer(Modifier.height(28.dp))
        }

        item("header") {
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "AI Brief is an independent journal about artificial intelligence. Original editorial explainers and briefings are published by Caceras, with sources you can check.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.widthIn(max = 520.dp),
            )
            Spacer(Modifier.height(36.dp))
        }

        item("sources") {
            Text(
                text = "Sources",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
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

/** Minimal text-first category filter row: all  research  products  policy  builders */
@Composable
private fun CategoryFilterRow(
    selectedCategory: NewsCategory,
    onSelectCategory: (NewsCategory) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        NewsCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .semantics { this.selected = selected }
                    .clickable(
                        role = Role.Tab,
                        onClick = {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelectCategory(category)
                        },
                    )
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun EditorialArticleRow(article: NewsArticle, onClick: () -> Unit) {
    StoryRow(article, onClick)
}

/** A readable native article with source links, local saving, and sharing. */
@Composable
private fun ArticleDetailScreen(
    article: NewsArticle,
    isSaved: Boolean,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onToggleSaved: () -> Unit,
    onBack: () -> Unit,
    onNavigateTab: (NavTab) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().wrapContentWidth().widthIn(max = 840.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = topPadding + 12.dp,
            end = 20.dp,
            bottom = bottomPadding + 44.dp,
        ),
    ) {
        item("nav") {
            MinimalTopNav(
                activeTab = null,
                onTabSelected = onNavigateTab,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onBack),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back to stories", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onToggleSaved) {
                    Icon(if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved" else "Save article")
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item("header") {
            SelectionContainer {
                Column {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(14.dp))
                    val meta = "${formatArticleDate(article.publishedAt)}  •  ${article.source}  •  ${article.readingTimeMinutes} min read"
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    article.author?.let { author ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "By $author",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (article.isOriginalPost && article.summary.isNotBlank()) {
                Text(article.summary, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 18.dp))
            }
            if (article.isOfflineBrief) Text("Offline explainer · background reading, not breaking news",
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(28.dp))
        }

        if (article.isOriginalPost || !article.imageUrl.isNullOrBlank()) {
            item("image") {
                ArticleArtwork(article, Modifier.fillMaxWidth(), featured = true)
                if (article.imageUrl.isNullOrBlank()) Text(
                    "Illustrative artwork generated with AI; not a reported event.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(28.dp))
            }
        }

        item("prose") {
            SelectionContainer {
                Column {
                    if (article.isOriginalPost) {
                        // An original post carries its own body; the summary is
                        // only the standfirst already shown in the feed.
                        article.blocks.forEachIndexed { index, block ->
                            if (index > 0) Spacer(Modifier.height(20.dp))
                            PostBlockContent(block)
                        }
                    } else {
                        Text(
                            text = article.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
        }

        item("actions") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(28.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                // An original post has no external source to open.
                if (article.url.isNotBlank()) Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                context.openExternalUrl(article.url)
                            },
                        ),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (article.isOfflineBrief) "Open context" else "Read original source",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSaved()
                    },
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from saved" else "Save article",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        context.shareArticle(article)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share article",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = if (article.isOriginalPost) {
                    "Written for AI Brief."
                } else {
                    "AI Brief links directly to original reporting and research. Content is supplied by the listed publishers."
                },
                style = MaterialTheme.typography.bodyMedium,
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

private fun Context.openExternalUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Safe fallback
    }
}

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
    val chooser = Intent.createChooser(shareIntent, "Share article")
    try {
        startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        // Safe fallback
    }
}

private fun Context.sendEmail(address: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "AI Brief feedback")
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Safe fallback
    }
}
