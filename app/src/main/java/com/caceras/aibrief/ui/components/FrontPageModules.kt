package com.caceras.aibrief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.caceras.aibrief.data.ModelRadar
import com.caceras.aibrief.data.ModelRankings
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.ui.theme.Brief
import com.caceras.aibrief.ui.theme.BriefShapes
import com.caceras.aibrief.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.US)

private fun Instant.shortDate() = SHORT_DATE.withZone(ZoneId.systemDefault()).format(this)

/** "Source: Artificial Analysis ↗" plus the date the figures were checked. */
@Composable
private fun SourceLine(source: String, checkedAt: Instant?, onOpen: () -> Unit) {
    val c = Brief.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.s)) {
        Text("Source: $source ↗", style = Brief.type.button, color = c.accent,
            modifier = Modifier.minimumInteractiveComponentSize().spokenAs("Source: $source")
                .clickable(role = Role.Button, onClickLabel = "Open in browser", onClick = onOpen))
        checkedAt?.let { Text("Checked ${it.shortDate()}", style = Brief.type.meta, color = c.inkMuted) }
    }
}

@Composable
private fun TextAction(label: String, spoken: String, onClick: () -> Unit) {
    Text(label, style = Brief.type.button, color = Brief.colors.accent,
        modifier = Modifier.padding(horizontal = Space.gutter).minimumInteractiveComponentSize().spokenAs(spoken)
            .clickable(role = Role.Button, onClick = onClick))
}

/**
 * The model leaderboard: rank, name, maker and score, with a bar scaled to the
 * leader. Always names its index and source, and links to it.
 */
@Composable
fun RankingsModule(rankings: ModelRankings, limit: Int, onOpenSource: () -> Unit, onSeeAll: (() -> Unit)?) {
    val c = Brief.colors
    val leader = rankings.entries.maxOf { it.score }.takeIf { it > 0 } ?: 1.0
    Column(Modifier.fillMaxWidth().background(c.card)) {
        SectionRibbon(rankings.title)
        Text(rankings.metric, style = Brief.type.meta, color = c.inkMuted,
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.m))
        rankings.entries.take(limit).forEach { model ->
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
                .padding(horizontal = Space.gutter, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically) {
                Text("${model.rank}", style = Brief.type.headline,
                    color = if (model.rank == 1) c.accent else c.inkMuted, modifier = Modifier.widthIn(min = 40.dp))
                Column(Modifier.weight(1f)) {
                    Text(model.name, style = Brief.type.rowTitle, color = c.ink, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(model.maker.takeIf { it.isNotBlank() }, "Open weights".takeIf { model.openWeights })
                        .joinToString(" · "), style = Brief.type.meta, color = c.inkMuted)
                    Spacer(Modifier.height(Space.xs))
                    Box(Modifier.fillMaxWidth((model.score / leader).toFloat().coerceIn(0.05f, 1f)).height(Space.xs)
                        .background(if (model.rank == 1) c.accent else c.rule, BriefShapes.pill))
                }
                Spacer(Modifier.width(Space.m))
                Text(String.format(Locale.US, "%.1f", model.score), style = Brief.type.headlineSmall, color = c.ink)
            }
            Rule(Modifier.padding(horizontal = Space.gutter))
        }
        if (onSeeAll != null && rankings.entries.size > limit) {
            TextAction("See all ${rankings.entries.size}  →", "See all ${rankings.entries.size} models", onSeeAll)
        }
        SourceLine(rankings.source, rankings.checkedAt, onOpenSource)
    }
}

/**
 * Newly listed models, with stealth models (unannounced, maker not named)
 * first and flagged in red. Each row opens the model's public listing.
 */
@Composable
fun RadarModule(radar: ModelRadar, limit: Int, onOpenUrl: (String) -> Unit, onSeeAll: (() -> Unit)?) {
    val c = Brief.colors
    Column(Modifier.fillMaxWidth().background(c.card)) {
        SectionRibbon(radar.title)
        Text("New and unannounced models, as listed on ${radar.source}. Stealth models are tested in public " +
            "before their makers are named.", style = Brief.type.meta, color = c.inkMuted,
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.m))
        radar.entries.take(limit).forEach { model ->
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
                .clickable(role = Role.Button, onClickLabel = "Open listing in browser") { onOpenUrl(model.url) }
                .padding(horizontal = Space.gutter, vertical = Space.m)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((if (model.stealth) "Stealth" else model.maker).uppercase(Locale.US), style = Brief.type.tag,
                        color = if (model.stealth) c.onBreaking else c.accent,
                        modifier = if (model.stealth) Modifier.background(c.breaking, BriefShapes.thumb)
                            .padding(horizontal = Space.s, vertical = Space.xxs) else Modifier)
                    model.listedOn?.let {
                        Spacer(Modifier.width(Space.s))
                        Text("Listed ${SHORT_DATE.format(it)}", style = Brief.type.meta, color = c.inkMuted)
                    }
                }
                Spacer(Modifier.height(Space.xs))
                Text(model.name, style = Brief.type.rowTitle, color = c.ink)
                if (model.note.isNotBlank()) {
                    Text(model.note, style = Brief.type.deck, color = c.inkMuted, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            Rule(Modifier.padding(horizontal = Space.gutter))
        }
        if (onSeeAll != null && radar.entries.size > limit) {
            TextAction("See all ${radar.entries.size}  →", "See all ${radar.entries.size} models", onSeeAll)
        }
        SourceLine(radar.source, radar.checkedAt) { onOpenUrl(radar.sourceUrl) }
    }
}

/**
 * Every part of a series in a sideways rail. All parts share one look, navy
 * cards with an orange part number, so a series is recognisable at a glance.
 */
@Composable
fun SeriesRail(parts: List<NewsArticle>, onOpen: (NewsArticle) -> Unit) {
    val c = Brief.colors
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.72f
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            parts.forEach { article ->
                val series = article.series ?: return@forEach
                Column(Modifier.width(cardWidth).heightIn(min = 260.dp).clip(BriefShapes.card).background(c.masthead)
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Button) { onOpen(article) }) {
                    ArticleArtwork(article, ratio = Ratio.HERO)
                    Column(Modifier.padding(Space.gutter)) {
                        Text("PART ${series.part} OF ${series.count}", style = Brief.type.tag, color = c.opinionAccent)
                        Spacer(Modifier.height(Space.s))
                        Text(article.title, style = Brief.type.headlineSmall, color = c.onMasthead, maxLines = 4,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                    }
                }
            }
        }
    }
}
