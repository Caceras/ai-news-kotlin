package com.caceras.aibrief.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caceras.aibrief.R
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Original illustrative still lifes, never photographs of reported events. */
@DrawableRes
private fun categoryArtwork(category: NewsCategory): Int = when (category) {
    NewsCategory.RESEARCH -> R.drawable.editorial_research
    NewsCategory.BUILDERS -> R.drawable.editorial_builders
    NewsCategory.POLICY -> R.drawable.editorial_policy
    else -> R.drawable.editorial_studio
}

@Composable
fun ArticleArtwork(article: NewsArticle, modifier: Modifier = Modifier, featured: Boolean = false) {
    val fallback = if (article.id == "2026-10-03-small-models") R.drawable.editorial_research
        else categoryArtwork(article.category)
    val ratio = if (featured) 16f / 9f else 1f
    if (!article.imageUrl.isNullOrBlank()) {
        AsyncArticleImage(article.imageUrl, null, modifier, ratio, fallbackResource = fallback)
    } else {
        Image(
            painter = painterResource(fallback),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxWidth().aspectRatio(ratio),
        )
    }
}

@Composable
fun FeatureStory(article: NewsArticle, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column {
            ArticleArtwork(article, featured = true)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    article.category.label.uppercase(Locale.US) + if (article.isOriginalPost) "  /  EXPLAINER" else "",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(article.title, style = MaterialTheme.typography.headlineMedium,
                    maxLines = 4, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Text(article.summary, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StoryProvenance(article, Modifier.weight(1f))
                    Text("Read story", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null,
                        Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StoryRow(article: NewsArticle, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ArticleArtwork(article, Modifier.width(84.dp).height(96.dp).clip(RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    article.category.label.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(5.dp))
                Text(article.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                StoryProvenance(article)
            }
        }
    }
}

@Composable
private fun StoryProvenance(article: NewsArticle, modifier: Modifier = Modifier) {
    val date = DateTimeFormatter.ofPattern("d MMM", Locale.US)
        .withZone(ZoneId.systemDefault()).format(article.publishedAt)
    Text(
        text = (if (article.isOriginalPost) "" else "${article.source} · ") +
            "$date  ·  ${article.readingTimeMinutes} min read" +
            (if (article.isOfflineBrief) " · Background reading" else ""),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun TopicsStrip(onSelect: (NewsCategory) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF082638)).padding(vertical = 28.dp)) {
        Text("A world worth understanding", style = MaterialTheme.typography.headlineMedium,
            color = Color.White, modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text("From the research lab to everyday life.", style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCBDBDF), modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NewsCategory.entries.filter { it != NewsCategory.ALL }.forEach { category ->
                Box(
                    Modifier.width(148.dp).height(188.dp).clip(RoundedCornerShape(3.dp))
                        .spokenAs("Explore ${category.label}")
                        .clickable(role = Role.Button) { onSelect(category) },
                ) {
                    Image(painterResource(categoryArtwork(category)), null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xE6081720)), startY = 80f)))
                    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Text(category.label, style = MaterialTheme.typography.titleLarge,
                            color = Color.White)
                        Spacer(Modifier.height(5.dp))
                        Text("Explore  →", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDAE5E8))
                    }
                }
            }
        }
    }
}
