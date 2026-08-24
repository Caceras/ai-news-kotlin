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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.ui.components.AsyncArticleImage
import com.caceras.aibrief.ui.theme.AiBriefTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SUPPORT_EMAIL = "rikicaceras@gmail.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiBriefTheme {
                val newsViewModel: NewsViewModel = viewModel()
                AiBriefApp(newsViewModel)
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
private fun AiBriefApp(viewModel: NewsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = selectedArticle != null || currentTab != NavTab.HOME) {
        if (selectedArticle != null) {
            selectedArticle = null
        } else {
            currentTab = NavTab.HOME
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = selectedArticle to currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screenTransition",
        ) { (article, tab) ->
            if (article != null) {
                ArticleDetailScreen(
                    article = article,
                    isSaved = state.savedArticles.any { it.id == article.id },
                    topPadding = topPadding,
                    bottomPadding = bottomPadding,
                    onBack = { selectedArticle = null },
                    onToggleSaved = { viewModel.toggleSaved(article) },
                    onNavigateTab = {
                        selectedArticle = null
                        currentTab = it
                    },
                )
            } else {
                when (tab) {
                    NavTab.HOME -> FeedScreen(
                        state = state,
                        currentTab = currentTab,
                        topPadding = topPadding,
                        bottomPadding = bottomPadding,
                        onTabSelected = {
                            if (it == NavTab.HOME) {
                                viewModel.refresh()
                            }
                            currentTab = it
                        },
                        onSelectCategory = viewModel::selectCategory,
                        onOpenArticle = { selectedArticle = it },
                        onToggleSaved = viewModel::toggleSaved,
                    )
                    NavTab.SAVED -> SavedScreen(
                        articles = state.savedArticles,
                        currentTab = currentTab,
                        topPadding = topPadding,
                        bottomPadding = bottomPadding,
                        onTabSelected = { currentTab = it },
                        onOpenArticle = { selectedArticle = it },
                    )
                    NavTab.ABOUT -> AboutScreen(
                        currentTab = currentTab,
                        topPadding = topPadding,
                        bottomPadding = bottomPadding,
                        onTabSelected = { currentTab = it },
                    )
                }
            }
        }
    }
}

/** Pure minimal top text navigation: home  saved  about */
@Composable
private fun MinimalTopNav(
    activeTab: NavTab?,
    isRefreshing: Boolean = false,
    onTabSelected: (NavTab) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavTextLink(
            text = "home",
            selected = activeTab == NavTab.HOME,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTabSelected(NavTab.HOME)
            },
        )
        NavTextLink(
            text = "saved",
            selected = activeTab == NavTab.SAVED,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTabSelected(NavTab.SAVED)
            },
        )
        NavTextLink(
            text = "about",
            selected = activeTab == NavTab.ABOUT,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTabSelected(NavTab.ABOUT)
            },
        )
        if (isRefreshing) {
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun FeedScreen(
    state: NewsUiState,
    currentTab: NavTab,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (NavTab) -> Unit,
    onSelectCategory: (NewsCategory) -> Unit,
    onOpenArticle: (NewsArticle) -> Unit,
    onToggleSaved: (NewsArticle) -> Unit,
) {
    val articles = state.articles.filter {
        state.selectedCategory == NewsCategory.ALL || it.category == state.selectedCategory
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp,
            top = topPadding + 28.dp,
            end = 28.dp,
            bottom = bottomPadding + 44.dp,
        ),
    ) {
        item("nav") {
            MinimalTopNav(
                activeTab = currentTab,
                isRefreshing = state.isRefreshing,
                onTabSelected = onTabSelected,
            )
            Spacer(Modifier.height(44.dp))
        }

        item("header") {
            Text(
                text = "AI Brief",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "A calm, text-first read on artificial intelligence models, research, and policy.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.widthIn(max = 520.dp),
            )
            Spacer(Modifier.height(28.dp))
            CategoryFilterRow(
                selectedCategory = state.selectedCategory,
                onSelectCategory = onSelectCategory,
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
                    text = "No articles found in this category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavedScreen(
    articles: List<NewsArticle>,
    currentTab: NavTab,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (NavTab) -> Unit,
    onOpenArticle: (NewsArticle) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp,
            top = topPadding + 28.dp,
            end = 28.dp,
            bottom = bottomPadding + 44.dp,
        ),
    ) {
        item("nav") {
            MinimalTopNav(
                activeTab = currentTab,
                onTabSelected = onTabSelected,
            )
            Spacer(Modifier.height(44.dp))
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
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onTabSelected: (NavTab) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp,
            top = topPadding + 28.dp,
            end = 28.dp,
            bottom = bottomPadding + 44.dp,
        ),
    ) {
        item("nav") {
            MinimalTopNav(
                activeTab = currentTab,
                onTabSelected = onTabSelected,
            )
            Spacer(Modifier.height(44.dp))
        }

        item("header") {
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "AI Brief is a quiet, independent reader for artificial intelligence research, product engineering, and policy.",
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
                text = "Live feeds are fetched directly from MIT News, Google AI Research, Hugging Face, and VentureBeat. Each story attributes its original author, date, and link.",
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
                text = "No accounts, no analytics tracking, no advertising SDKs. Saved reads remain entirely on your device. The app connects only to fetch public RSS feeds.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
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
                text = category.label.lowercase(Locale.US),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
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

/** Clean editorial row matching the reference screenshot: Date on left, Title on right */
@Composable
private fun EditorialArticleRow(
    article: NewsArticle,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = formatShortDate(article.publishedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(125.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Article Reader View matching Reference 3 layout */
@Composable
private fun ArticleDetailScreen(
    article: NewsArticle,
    isSaved: Boolean,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onToggleSaved: () -> Unit,
    onNavigateTab: (NavTab) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp,
            top = topPadding + 28.dp,
            end = 28.dp,
            bottom = bottomPadding + 44.dp,
        ),
    ) {
        item("nav") {
            MinimalTopNav(
                activeTab = null,
                onTabSelected = onNavigateTab,
            )
            Spacer(Modifier.height(44.dp))
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
            Spacer(Modifier.height(28.dp))
        }

        if (!article.imageUrl.isNullOrBlank()) {
            item("image") {
                AsyncArticleImage(
                    imageUrl = article.imageUrl,
                    contentDescription = "Cover image for ${article.title}",
                    modifier = Modifier.fillMaxWidth(),
                    aspectRatio = 16f / 9f,
                )
                Spacer(Modifier.height(28.dp))
            }
        }

        item("prose") {
            SelectionContainer {
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(36.dp))
        }

        item("actions") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
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
                Spacer(Modifier.weight(1f))
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
            Spacer(Modifier.height(32.dp))
            Text(
                text = "AI Brief links directly to original reporting and research. Content is supplied by the listed publishers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatArticleDate(instant: Instant): String = DateTimeFormatter
    .ofPattern("MMMM d, yyyy", Locale.US)
    .withZone(ZoneId.systemDefault())
    .format(instant)

private fun formatShortDate(instant: Instant): String = DateTimeFormatter
    .ofPattern("MMMM d, yyyy", Locale.US)
    .withZone(ZoneId.systemDefault())
    .format(instant)

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
        putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.url}")
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
