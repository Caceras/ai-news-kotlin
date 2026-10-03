package com.caceras.aibrief.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caceras.aibrief.R
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ArticleArtwork(article: NewsArticle, modifier: Modifier = Modifier, featured: Boolean = false) {
    if (!article.imageUrl.isNullOrBlank()) {
        AsyncArticleImage(article.imageUrl, null, modifier, if (featured) 16f / 9f else 1f)
    } else if (featured) {
        Image(
            painter = painterResource(R.drawable.editorial_studio),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
    } else {
        TopicIllustration(article.category, modifier.aspectRatio(1f))
    }
}

@Composable
fun FeatureStory(article: NewsArticle, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
    ) {
        Column {
            ArticleArtwork(article, featured = true)
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "THE READING ROOM · ${article.category.label.uppercase(Locale.US)}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(article.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                Text(article.summary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                StoryProvenance(article)
                Spacer(Modifier.height(16.dp))
                Text("READ THE STORY  →", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StoryRow(article: NewsArticle, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            ArticleArtwork(article, Modifier.size(88.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    article.category.label.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(article.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                StoryProvenance(article)
            }
        }
    }
}

@Composable
private fun StoryProvenance(article: NewsArticle) {
    val date = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
        .withZone(ZoneId.systemDefault()).format(article.publishedAt)
    Text(
        text = "${article.source} · $date · ${article.readingTimeMinutes} min",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (article.isOfflineBrief) Text(
        "Offline explainer · background reading",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun TopicsStrip(onSelect: (NewsCategory) -> Unit) {
    Column {
        Text("Explore artificial intelligence", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text("A little more context. A clearer point of view.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewsCategory.entries.filter { it != NewsCategory.ALL }.forEach { category ->
                Column(
                    Modifier.width(154.dp)
                        .background(Color(0xFF082638))
                        .clickable(role = Role.Button) { onSelect(category) },
                ) {
                    TopicIllustration(category, Modifier.fillMaxWidth().height(132.dp))
                    Column(Modifier.padding(14.dp)) {
                        Text(category.label, style = MaterialTheme.typography.titleLarge,
                            color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("EXPLORE  →", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD9E5E8))
                    }
                }
            }
        }
    }
}

/** Original, decorative artwork. It carries no factual information. */
@Composable
private fun TopicIllustration(category: NewsCategory, modifier: Modifier = Modifier) {
    val palette = when (category) {
        NewsCategory.RESEARCH -> Color(0xFFB8C9B7) to Color(0xFF345849)
        NewsCategory.PRODUCTS -> Color(0xFFD9C2A3) to Color(0xFF836344)
        NewsCategory.POLICY -> Color(0xFFB4C4CF) to Color(0xFF2D5067)
        else -> Color(0xFFCFB2A1) to Color(0xFF835747)
    }
    Box(modifier.background(palette.first), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension
            val center = Offset(size.width * .5f, size.height * .48f)
            drawCircle(palette.second.copy(alpha = .12f), unit * .37f, center)
            for (i in 1..4) {
                drawCircle(palette.second.copy(alpha = .7f), unit * (.07f * i), center,
                    style = Stroke(unit * .009f))
            }
            drawLine(palette.second, Offset(unit * .13f, size.height * .8f),
                Offset(size.width - unit * .13f, size.height * .2f), unit * .012f)
            drawCircle(palette.second, unit * .048f,
                Offset(size.width * .68f, size.height * .3f))
        }
    }
}
