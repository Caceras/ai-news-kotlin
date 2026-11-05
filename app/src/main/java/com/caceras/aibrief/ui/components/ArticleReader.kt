package com.caceras.aibrief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.StoryFormat
import com.caceras.aibrief.ui.theme.Brief
import com.caceras.aibrief.ui.theme.BriefShapes
import com.caceras.aibrief.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Every date in the reader reads the same way: "March 4, 2026". */
private val READER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
private val READER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US)

// The zone is resolved per call, so dates stay right if the device changes time zone.
private fun Instant.readerDate() = READER_DATE.withZone(ZoneId.systemDefault()).format(this)
private fun Instant.readerTime() = READER_TIME.withZone(ZoneId.systemDefault()).format(this)

/** "By Name · March 4, 2026 · Updated Mar 5, 14:05 · 5 min read" */
fun NewsArticle.bylineText(): String = listOfNotNull(
    author?.let { "By $it" } ?: source,
    publishedAt.readerDate(),
    updatedAt?.let { "Updated ${it.readerTime()}" },
    "$readingTimeMinutes min read",
).joinToString(" · ")

/** The full story. Each format opens with its own header; the body is the same blocks everywhere. */
@Composable
fun ArticleReader(
    article: NewsArticle,
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    onShare: () -> Unit,
    onOpenSource: () -> Unit,
    onBack: () -> Unit,
    /** Every part of this story's series, in order, when it belongs to one. */
    seriesParts: List<NewsArticle> = emptyList(),
    onOpenArticle: (NewsArticle) -> Unit = {},
) {
    val c = Brief.colors
    val haptic = LocalHapticFeedback.current
    val opinion = article.format == StoryFormat.OPINION
    Column(Modifier.fillMaxSize().background(c.card).wrapContentWidth().widthIn(max = 840.dp)) {
        Row(Modifier.fillMaxWidth().background(c.masthead).padding(horizontal = Space.xs),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to stories", tint = c.onMasthead)
            }
            BrandMark(c.onMasthead, Modifier.weight(1f))
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleSaved()
            }) {
                Icon(if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (isSaved) "Remove from saved" else "Save article", tint = c.onMasthead)
            }
            IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, "Share article", tint = c.onMasthead) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = Space.section)) {
            item("header") {
                when {
                    opinion -> OpinionHeader(article)
                    article.isDesigned -> DesignedHeader(article)
                    else -> NewsHeader(article)
                }
            }
            item("prose") {
                SelectionContainer {
                    Column(Modifier.padding(horizontal = Space.l)) {
                        if (article.isOriginalPost) article.blocks.forEachIndexed { index, block ->
                            if (index > 0) Spacer(Modifier.height(Space.l))
                            PostBlockContent(block, opinion = opinion)
                        } else Text(article.summary, style = Brief.type.body, color = c.ink)
                    }
                }
            }
            if (article.series != null && seriesParts.size > 1) item("series") {
                SeriesContents(article, seriesParts, onOpenArticle)
            }
            item("footer") {
                Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.xxl)) {
                    Rule()
                    Spacer(Modifier.height(Space.l))
                    if (article.url.isNotBlank()) {
                        Row(Modifier.minimumInteractiveComponentSize().border(Space.hairline, c.ink, BriefShapes.button)
                            .clickable(role = Role.Button, onClick = onOpenSource)
                            .padding(horizontal = Space.gutter, vertical = Space.m),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(if (article.isOfflineBrief) "Open context" else "Read original source",
                                style = Brief.type.button, color = c.ink)
                            Spacer(Modifier.width(Space.s))
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp), tint = c.ink)
                        }
                        Spacer(Modifier.height(Space.m))
                    }
                    Text(when {
                        article.format == StoryFormat.SPONSORED -> "Paid content from ${article.sponsor}, published by AI Brief."
                        article.isOriginalPost -> "Published by AI Brief · Caceras"
                        else -> "Reporting and research from ${article.source}."
                    }, style = Brief.type.meta, color = c.inkMuted)
                    Spacer(Modifier.height(Space.l))
                    Text("Back to stories  ←", style = Brief.type.button, color = c.accent,
                        modifier = Modifier.minimumInteractiveComponentSize().spokenAs("Back to stories")
                            .clickable(role = Role.Button, onClick = onBack))
                }
            }
        }
    }
}

@Composable
private fun NewsHeader(article: NewsArticle) {
    val c = Brief.colors
    Column {
        if (article.format == StoryFormat.BREAKING) {
            Collage(article)
            Row(Modifier.fillMaxWidth().background(c.breaking).padding(horizontal = Space.l, vertical = Space.s)) {
                Text("BREAKING · ${(article.updatedAt ?: article.publishedAt).readerTime()}",
                    style = Brief.type.tag, color = c.onBreaking)
            }
        } else if (article.isOriginalPost || !article.imageUrl.isNullOrBlank()) {
            ArticleArtwork(article)
            val credit = if (article.imageUrl.isNullOrBlank()) "AI-generated editorial illustration"
                else article.imageCredit
            credit?.let {
                Text(it, style = Brief.type.meta, color = c.inkMuted,
                    modifier = Modifier.padding(start = Space.l, end = Space.l, top = Space.s))
            }
        }
        SelectionContainer {
            Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.l, bottom = Space.xl)) {
                article.series?.let {
                    Text(it.label.uppercase(Locale.US), style = Brief.type.tag, color = c.opinionAccent)
                    Spacer(Modifier.height(Space.xs))
                }
                Tag(article)
                Spacer(Modifier.height(Space.s))
                Headline(article, Brief.type.display, maxLines = Int.MAX_VALUE)
                Spacer(Modifier.height(Space.m))
                Deck(article.summary, maxLines = Int.MAX_VALUE)
                Spacer(Modifier.height(Space.gutter))
                Text(article.bylineText(), style = Brief.type.meta, color = c.inkMuted)
                if (article.isOfflineBrief) Text("Offline explainer · background reading",
                    style = Brief.type.meta, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs))
            }
        }
    }
}

@Composable
private fun OpinionHeader(article: NewsArticle) {
    val c = Brief.colors
    Column(Modifier.fillMaxWidth().background(c.opinionPaper)
        .padding(start = Space.l, end = Space.l, top = Space.xl, bottom = Space.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Portrait(article, 96.dp)
            Spacer(Modifier.width(Space.gutter))
            Column {
                Text(StoryFormat.OPINION.label.uppercase(Locale.US), style = Brief.type.label, color = c.opinionAccent)
                Spacer(Modifier.height(Space.xs))
                Text(article.author ?: article.source, style = Brief.type.rowTitle, color = c.ink)
                article.authorRole?.let { Text(it, style = Brief.type.meta, color = c.inkMuted) }
            }
        }
        Spacer(Modifier.height(Space.xl))
        SelectionContainer {
            Column {
                Text(article.title, style = Brief.type.opinionHeadline, color = c.ink,
                    modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Space.m))
                Text(article.summary, style = Brief.type.quote, color = c.inkMuted)
                Spacer(Modifier.height(Space.gutter))
                Text(article.bylineText(), style = Brief.type.meta, color = c.inkMuted)
            }
        }
        Spacer(Modifier.height(Space.m))
        Text("Opinion: the views in this column are the author's own.", style = Brief.type.meta,
            color = c.inkMuted)
    }
    Spacer(Modifier.height(Space.xl))
}

@Composable
private fun DesignedHeader(article: NewsArticle) {
    val c = Brief.colors
    Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.gutter, bottom = Space.xl)) {
        DesignedCard(article, onClick = null)
        Spacer(Modifier.height(Space.gutter))
        if (article.format == StoryFormat.SPONSORED) {
            Box(Modifier.fillMaxWidth().background(c.paper, BriefShapes.button).padding(Space.m)) {
                Text("Sponsored. ${article.sponsor} paid for this content. It is labelled so you can tell it apart from AI Brief's own reporting.",
                    style = Brief.type.meta, color = c.ink)
            }
            Spacer(Modifier.height(Space.m))
        }
        Text(article.bylineText(), style = Brief.type.meta, color = c.inkMuted)
    }
}

/** "In this series": every part, this one marked, the others one tap away. */
@Composable
private fun SeriesContents(article: NewsArticle, parts: List<NewsArticle>, onOpen: (NewsArticle) -> Unit) {
    val c = Brief.colors
    val series = article.series ?: return
    Column(Modifier.padding(start = Space.l, end = Space.l, top = Space.xxl).fillMaxWidth()
        .background(c.masthead, BriefShapes.card).padding(Space.gutter)) {
        Text("IN THIS SERIES", style = Brief.type.tag, color = c.opinionAccent)
        Spacer(Modifier.height(Space.xs))
        Text(series.title, style = Brief.type.headlineSmall, color = c.onMasthead,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        parts.forEach { part ->
            val current = part.id == article.id
            Row(Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                .then(if (current) Modifier else Modifier.clickable(role = Role.Button) { onOpen(part) })
                .padding(vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                Text("${part.series?.part}", style = Brief.type.headline,
                    color = if (current) c.opinionAccent else c.onMasthead.copy(alpha = 0.6f),
                    modifier = Modifier.width(Space.xxl))
                Text(part.title + if (current) "  (reading now)" else "", style = Brief.type.rowTitle,
                    color = c.onMasthead)
            }
        }
    }
}
