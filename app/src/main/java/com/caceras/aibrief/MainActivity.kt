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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caceras.aibrief.data.FeedFreshness
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

private enum class Destination {
    FEED,
    SAVED,
}

@Composable
private fun AiBriefApp(viewModel: NewsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.FEED) }
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedArticle != null || showAbout) {
        if (selectedArticle != null) {
            selectedArticle = null
        } else {
            showAbout = false
        }
    }

    when {
        selectedArticle != null -> {
            val article = requireNotNull(selectedArticle)
            ArticleDetailScreen(
                article = article,
                isSaved = state.savedArticles.any { it.id == article.id },
                onBack = { selectedArticle = null },
                onToggleSaved = { viewModel.toggleSaved(article) },
            )
        }
        showAbout -> AboutScreen(onBack = { showAbout = false })
        else -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                MinimalNavigation(
                    destination = destination,
                    savedCount = state.savedArticles.size,
                    onDestinationSelected = { destination = it },
                )
            },
        ) { padding ->
            when (destination) {
                Destination.FEED -> FeedScreen(
                    state = state,
                    contentPadding = padding,
                    onSelectCategory = viewModel::selectCategory,
                    onRefresh = viewModel::refresh,
                    onOpenArticle = { selectedArticle = it },
                    onToggleSaved = viewModel::toggleSaved,
                    onOpenAbout = { showAbout = true },
                )
                Destination.SAVED -> SavedScreen(
                    articles = state.savedArticles,
                    contentPadding = padding,
                    onOpenArticle = { selectedArticle = it },
                    onToggleSaved = viewModel::toggleSaved,
                )
            }
        }
    }
}

@Composable
private fun FeedScreen(
    state: NewsUiState,
    contentPadding: PaddingValues,
    onSelectCategory: (NewsCategory) -> Unit,
    onRefresh: () -> Unit,
    onOpenArticle: (NewsArticle) -> Unit,
    onToggleSaved: (NewsArticle) -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val articles = state.articles.filter {
        state.selectedCategory == NewsCategory.ALL || it.category == state.selectedCategory
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
    ) {
        item("masthead") {
            Masthead(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                onOpenAbout = onOpenAbout,
            )
            Spacer(Modifier.height(48.dp))
            Text(
                text = "AI, without the noise.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A calm read on models, makers, and the rules shaping them.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 480.dp),
            )
            Spacer(Modifier.height(28.dp))
            FeedStatus(freshness = state.freshness, lastUpdated = state.lastUpdated)
            Spacer(Modifier.height(28.dp))
            CategoryBar(
                selectedCategory = state.selectedCategory,
                onSelectCategory = onSelectCategory,
            )
            Spacer(Modifier.height(34.dp))
        }

        if (articles.isNotEmpty()) {
            item("lead") {
                LeadStory(
                    article = articles.first(),
                    isSaved = state.savedArticles.any { it.id == articles.first().id },
                    onOpen = { onOpenArticle(articles.first()) },
                    onToggleSaved = { onToggleSaved(articles.first()) },
                )
                Spacer(Modifier.height(48.dp))
                SectionHeading(title = "Latest", count = articles.size)
            }

            items(articles.drop(1), key = { it.id }) { article ->
                ArticleRow(
                    article = article,
                    isSaved = state.savedArticles.any { it.id == article.id },
                    onOpen = { onOpenArticle(article) },
                    onToggleSaved = { onToggleSaved(article) },
                )
            }
        } else {
            item("empty-filter") {
                EmptyState(
                    title = "Nothing in this cut.",
                    detail = "Choose another topic to see the rest of today's brief.",
                )
            }
        }
        item("feed-footer") {
            AppFooter(onContact = { context.sendEmail(SUPPORT_EMAIL) })
        }
    }
}

@Composable
private fun SavedScreen(
    articles: List<NewsArticle>,
    contentPadding: PaddingValues,
    onOpenArticle: (NewsArticle) -> Unit,
    onToggleSaved: (NewsArticle) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            end = 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
    ) {
        item("saved-header") {
            Text(
                text = "Your shelf",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A small collection of reads worth returning to.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(44.dp))
        }
        if (articles.isEmpty()) {
            item("saved-empty") {
                EmptyState(
                    title = "Nothing saved yet.",
                    detail = "Keep the pieces that deserve a second read. They stay here on this device.",
                )
            }
        } else {
            item("saved-count") {
                SectionHeading(title = "Saved", count = articles.size)
            }
            items(articles, key = { it.id }) { article ->
                ArticleRow(
                    article = article,
                    isSaved = true,
                    onOpen = { onOpenArticle(article) },
                    onToggleSaved = { onToggleSaved(article) },
                )
            }
        }
        item("saved-footer") {
            AppFooter(onContact = { context.sendEmail(SUPPORT_EMAIL) })
        }
    }
}

@Composable
private fun Masthead(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "AI / BRIEF",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenAbout()
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About AI Brief",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRefresh()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh news",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = isRefreshing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onBackground,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun FeedStatus(freshness: FeedFreshness, lastUpdated: Instant?) {
    val markerColor = when (freshness) {
        FeedFreshness.LIVE -> Color(0xFF296E4D)
        FeedFreshness.CACHED -> Color(0xFF8A6F3D)
        FeedFreshness.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val status = when (freshness) {
        FeedFreshness.LIVE -> "LIVE SOURCES"
        FeedFreshness.CACHED -> "CACHED EDITION"
        FeedFreshness.OFFLINE -> "OFFLINE READING LIST"
    }
    val detail = lastUpdated?.let { "Updated ${formatShortDate(it)}" }
        ?: "Available without a connection"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(markerColor),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryBar(
    selectedCategory: NewsCategory,
    onSelectCategory: (NewsCategory) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        NewsCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            Column(
                modifier = Modifier
                    .clickable(
                        role = Role.Tab,
                        onClick = {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelectCategory(category)
                        },
                    )
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(1.5.dp)
                        .fillMaxWidth()
                        .background(if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun LeadStory(
    article: NewsArticle,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpen()
                },
            )
            .padding(vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .width(3.dp)
                    .background(MaterialTheme.colorScheme.onBackground),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "LEAD STORY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            SaveButton(isSaved = isSaved, onClick = onToggleSaved)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!article.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(18.dp))
            AsyncArticleImage(
                imageUrl = article.imageUrl,
                contentDescription = "Image for ${article.title}",
                modifier = Modifier.fillMaxWidth(),
                aspectRatio = 16f / 9f,
            )
            Spacer(Modifier.height(18.dp))
        } else {
            Spacer(Modifier.height(14.dp))
        }
        Text(
            text = article.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(20.dp))
        StoryMeta(article = article)
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SectionHeading(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArticleRow(
    article: NewsArticle,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpen()
                },
            )
            .padding(vertical = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StoryMeta(article = article)
                Spacer(Modifier.height(9.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!article.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.width(14.dp))
                AsyncArticleImage(
                    imageUrl = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    aspectRatio = 1f,
                )
            }
            Spacer(Modifier.width(10.dp))
            SaveButton(isSaved = isSaved, onClick = onToggleSaved)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StoryMeta(article: NewsArticle) {
    val meta = "${article.source.uppercase(Locale.US)} / ${article.category.label.uppercase(Locale.US)} / ${formatShortDate(article.publishedAt)} / ${article.readingTimeMinutes} MIN READ"
    Text(
        text = meta,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SaveButton(isSaved: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
    ) {
        Icon(
            imageVector = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isSaved) "Remove saved article" else "Save article",
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(32.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
        )
    }
}

@Composable
private fun AppFooter(onContact: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp, bottom = 12.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onContact()
                    },
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Contact the editor",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Corrections, source requests, and feedback are welcome.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MinimalNavigation(
    destination: Destination,
    savedCount: Int,
    onDestinationSelected: (Destination) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 12.dp,
                    end = 24.dp,
                    bottom = 12.dp + navigationInset,
                ),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            NavigationLabel(
                label = "Feed",
                selected = destination == Destination.FEED,
                onClick = {
                    if (destination != Destination.FEED) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onDestinationSelected(Destination.FEED)
                },
            )
            NavigationLabel(
                label = if (savedCount == 0) "Saved" else "Saved $savedCount",
                selected = destination == Destination.SAVED,
                onClick = {
                    if (destination != Destination.SAVED) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onDestinationSelected(Destination.SAVED)
                },
            )
        }
    }
}

@Composable
private fun NavigationLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .wrapContentWidth()
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent),
        )
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            end = 24.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 40.dp,
        ),
    ) {
        item("about") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "AI / BRIEF",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(56.dp))
            Text(
                text = "Trust, shown.",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "A quiet reader for the people and ideas moving AI forward.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            AboutSection(
                title = "Sources",
                body = "AI Brief reads public feeds from MIT News, Google AI, Hugging Face, and VentureBeat. Every live story keeps its original publisher, publication date, and link.",
            )
            AboutSection(
                title = "Editorial notes",
                body = "When a connection is unavailable, clearly labeled AI Brief notes keep the app useful. They are context, not third-party reporting.",
            )
            AboutSection(
                title = "Privacy",
                body = "No account, analytics, ads, or data sale. Saved reads stay on this device. The app contacts listed publishers only to refresh stories and load source images.",
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Email the editor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = SUPPORT_EMAIL,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "For corrections, source requests, privacy questions, or support.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutSection(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArticleDetailScreen(
    article: NewsArticle,
    isSaved: Boolean,
    onBack: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 24.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,
            end = 24.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 40.dp,
        ),
    ) {
        item("detail-toolbar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "AI / BRIEF",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        context.shareArticle(article)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share article",
                    )
                }
                SaveButton(isSaved = isSaved, onClick = onToggleSaved)
            }
            Spacer(Modifier.height(58.dp))
            Text(
                text = "${article.source.uppercase(Locale.US)} / ${article.category.label.uppercase(Locale.US)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "${formatArticleDate(article.publishedAt)} • ${article.readingTimeMinutes} min read",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            article.author?.let { author ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "By $author",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!article.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(28.dp))
                AsyncArticleImage(
                    imageUrl = article.imageUrl,
                    contentDescription = "Image for ${article.title}",
                    modifier = Modifier.fillMaxWidth(),
                    aspectRatio = 16f / 9f,
                )
            }
            Spacer(Modifier.height(38.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(28.dp))
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = if (article.isOfflineBrief) {
                    "Open the supporting reading for more context."
                } else {
                    "Open the source for the complete article and its original context."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            SourceLink(
                label = if (article.isOfflineBrief) "Open supporting reading" else "Read original source",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    context.openExternalUrl(article.url)
                },
            )
            Spacer(Modifier.height(40.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(22.dp))
            Text(
                text = if (article.isOfflineBrief) {
                    "This offline AI Brief is editorial context. The link provides further reading."
                } else {
                    "AI Brief links to original reporting and research. Headlines and descriptions are supplied by the listed sources."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceLink(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
            )
        }
    }
}

private fun formatArticleDate(instant: Instant): String = DateTimeFormatter
    .ofPattern("MMMM d, yyyy", Locale.US)
    .withZone(ZoneId.systemDefault())
    .format(instant)

private fun formatShortDate(instant: Instant): String = DateTimeFormatter
    .ofPattern("MMM d, yyyy", Locale.US)
    .withZone(ZoneId.systemDefault())
    .format(instant)

private fun Context.openExternalUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Devices without a compatible browser simply keep the reader in the app.
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
        // Devices without a share target simply keep the reader in the app.
    }
}

private fun Context.sendEmail(address: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "AI Brief feedback")
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Devices without an email client simply keep the reader in the app.
    }
}
