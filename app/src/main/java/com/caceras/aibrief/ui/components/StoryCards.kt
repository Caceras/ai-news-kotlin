package com.caceras.aibrief.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.caceras.aibrief.R
import com.caceras.aibrief.data.NewsArticle
import com.caceras.aibrief.data.NewsCategory
import com.caceras.aibrief.data.StoryFormat
import com.caceras.aibrief.ui.theme.Brief
import com.caceras.aibrief.ui.theme.BriefShapes
import com.caceras.aibrief.ui.theme.Space
import com.caceras.aibrief.ui.theme.cardPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

/** Original illustrations used when a story has no picture of its own. Never photographs of real events. */
@DrawableRes
fun categoryArtwork(category: NewsCategory): Int = when (category) {
    NewsCategory.RESEARCH -> R.drawable.editorial_research
    NewsCategory.BUILDERS -> R.drawable.editorial_builders
    NewsCategory.POLICY -> R.drawable.editorial_policy
    else -> R.drawable.editorial_studio
}

/** Photo ratios. Every picture in the app uses one of these. */
object Ratio {
    const val HERO = 16f / 10f
    /** The lead is taller than a headline photo, so the page opens on a picture. */
    const val LEAD = 4f / 3f
    const val PAIR = 4f / 3f
    const val THUMB = 4f / 3f
    const val VIDEO = 16f / 9f
}

/** A story's cover, or its topic illustration when it has none or the download fails. */
@Composable
fun ArticleArtwork(
    article: NewsArticle,
    modifier: Modifier = Modifier,
    ratio: Float? = Ratio.HERO,
    url: String? = article.imageUrl,
) {
    val fallback = categoryArtwork(article.category)
    if (!url.isNullOrBlank()) {
        AsyncArticleImage(url, null, modifier, ratio, fallbackResource = fallback)
    } else {
        AsyncArticleImage(bundledArt(fallback), null, modifier, ratio)
    }
}

/**
 * Breaking news cover: one picture, two side by side, or one large beside two
 * stacked. Pictures are separated by a thin paper-coloured seam.
 */
@Composable
fun Collage(article: NewsArticle, modifier: Modifier = Modifier, ratio: Float? = Ratio.HERO) {
    val pictures = article.pictures
    Box(modifier.then(if (ratio != null) Modifier.fillMaxWidth().aspectRatio(ratio) else Modifier)
        .background(Brief.colors.paper)) {
        when (pictures.size) {
            0, 1 -> ArticleArtwork(article, Modifier.fillMaxSize(), ratio = null)
            2 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                pictures.forEach { ArticleArtwork(article, Modifier.weight(1f).fillMaxHeight(), null, it) }
            }
            else -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                ArticleArtwork(article, Modifier.weight(1.4f).fillMaxHeight(), null, pictures[0])
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    ArticleArtwork(article, Modifier.weight(1f).fillMaxWidth(), null, pictures[1])
                    ArticleArtwork(article, Modifier.weight(1f).fillMaxWidth(), null, pictures[2])
                }
            }
        }
    }
}

/** The red lead-in a headline carries, when the post has one. Breaking news says so with its flag and tag instead. */
fun NewsArticle.kickerText(): String? = kicker

/** The headline with its kicker set in red before it, as one line of type. */
@Composable
fun kickerHeadline(article: NewsArticle): AnnotatedString {
    val red = Brief.colors.breakingInk
    return buildAnnotatedString {
        article.kickerText()?.let { kicker ->
            withStyle(SpanStyle(color = red)) { append(kicker.uppercase(Locale.US)); append(": ") }
        }
        append(article.title)
    }
}

/**
 * Stories this reader has opened. Provided around the front page and Explore,
 * not the reader, so headlines there turn muted once read.
 */
val LocalReadStories = staticCompositionLocalOf<Set<String>> { emptySet() }

@Composable
fun Headline(article: NewsArticle, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 4,
             color: Color? = null) {
    val read = article.id in LocalReadStories.current
    Text(kickerHeadline(article), style = style,
        color = color ?: if (read) Brief.colors.inkMuted else Brief.colors.ink, maxLines = scaledLines(maxLines),
        overflow = TextOverflow.Ellipsis, modifier = modifier.semantics {
            heading()
            if (read) stateDescription = "Read"
        })
}

/**
 * How many lines a capped headline or deck may use at the reader's font scale.
 * Caps are drawn for default text; at 200 % system text a four-line headline
 * needs about eight, so the cap grows with the scale and the card grows with it
 * instead of cutting the words off.
 */
fun linesForScale(lines: Int, fontScale: Float): Int =
    if (lines == Int.MAX_VALUE || fontScale <= 1f) lines else ceil(lines * fontScale).toInt()

@Composable
@ReadOnlyComposable
fun scaledLines(lines: Int): Int = linesForScale(lines, LocalDensity.current.fontScale)

/** The one-line follow-up under a headline. The arrow is decoration and is not read aloud. */
@Composable
fun Deck(text: String, modifier: Modifier = Modifier, maxLines: Int = 2, color: Color = Brief.colors.ink) {
    if (text.isBlank()) return
    Row(modifier) {
        Text("➤", style = Brief.type.deck, color = color, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.width(Space.s))
        Text(text, style = Brief.type.deck, color = color, maxLines = scaledLines(maxLines), overflow = TextOverflow.Ellipsis)
    }
}

/** The small caps label above a story: its format and topic, or its sponsor. */
fun NewsArticle.tagText(): String = when (format) {
    StoryFormat.SPONSORED -> "Sponsored · ${sponsor.orEmpty()}"
    StoryFormat.NEWS -> category.takeIf { it != NewsCategory.ALL }?.label ?: "News"
    StoryFormat.REVIEW -> listOfNotNull(format.label, rating?.let(::stars)).joinToString(" · ")
    else -> listOfNotNull(format.label, category.takeIf { it != NewsCategory.ALL }?.label).joinToString(" · ")
}.uppercase(Locale.US)

/**
 * The label above a story in the reader. A breaking story's red band already
 * says "Breaking", so its label names only the topic (or nothing) instead of
 * saying it twice, on screen and in TalkBack.
 */
fun NewsArticle.readerTagText(): String? =
    if (format == StoryFormat.BREAKING) category.takeIf { it != NewsCategory.ALL }?.label?.uppercase(Locale.US)
    else tagText()

@Composable
fun Tag(article: NewsArticle, modifier: Modifier = Modifier, color: Color = Brief.colors.accent,
        text: String = article.tagText()) {
    // A sponsored label wraps rather than hide who paid; other labels stay on one line.
    Text(text, style = Brief.type.tag, color = color,
        maxLines = if (article.format == StoryFormat.SPONSORED) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** "★★★★☆" for four out of five. */
fun stars(rating: Int): String = "★".repeat(rating.coerceIn(0, 5)) + "☆".repeat(5 - rating.coerceIn(0, 5))

private val CARD_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.US)
private val CARD_DATE_WITH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

/**
 * "4 Oct" this year, "4 Oct 2025" otherwise, so a story from last year never
 * passes for this week's on the front page.
 */
internal fun cardDate(publishedAt: Instant, zone: ZoneId, today: LocalDate): String {
    val day = publishedAt.atZone(zone).toLocalDate()
    return (if (day.year == today.year) CARD_DATE else CARD_DATE_WITH_YEAR).format(day)
}

/** "4 Oct · 5 min read" */
fun NewsArticle.metaText(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
    val date = cardDate(publishedAt, zone, today)
    return listOfNotNull(
        source.takeUnless { isOriginalPost },
        date,
        "$readingTimeMinutes min read",
        "Background reading".takeIf { isOfflineBrief },
    ).joinToString(" · ")
}

@Composable
fun Meta(article: NewsArticle, modifier: Modifier = Modifier, color: Color = Brief.colors.inkMuted) {
    Text(article.metaText(), style = Brief.type.meta, color = color, modifier = modifier)
}

private fun Modifier.story(onClick: () -> Unit) =
    semantics(mergeDescendants = true) {}.clickable(role = Role.Button, onClickLabel = READ_STORY, onClick = onClick)

/** What TalkBack says a story tap does: "Double tap to read story". */
const val READ_STORY = "Read story"

/** A small red "BREAKING" flag laid over the corner of a collage. */
@Composable
private fun BreakingFlag(modifier: Modifier = Modifier) {
    Text(StoryFormat.BREAKING.label.uppercase(Locale.US), style = Brief.type.tag, color = Brief.colors.onBreaking,
        modifier = modifier.background(Brief.colors.breaking).padding(horizontal = Space.s, vertical = Space.xs))
}

/** The top story. Each format leads in its own way. */
@Composable
fun LeadStory(article: NewsArticle, onClick: () -> Unit) = when {
    article.format == StoryFormat.OPINION -> OpinionCard(article, Modifier.padding(Space.gutter), onClick)
    article.isDesigned -> DesignedCard(article, Modifier.padding(Space.gutter), onClick)
    else -> BleedLead(article, onClick)
}

/**
 * The picture runs edge to edge with nothing on it, and the big headline sits
 * directly under it, so picture and headline read as one unit.
 */
@Composable
private fun BleedLead(article: NewsArticle, onClick: () -> Unit) {
    val c = Brief.colors
    Column(Modifier.fillMaxWidth().background(c.card).story(onClick)) {
        Box {
            if (article.format == StoryFormat.BREAKING) {
                Collage(article, ratio = Ratio.LEAD)
                BreakingFlag(Modifier.align(Alignment.TopStart).padding(Space.m))
            } else {
                ArticleArtwork(article, ratio = Ratio.LEAD)
            }
        }
        Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.s, bottom = Space.l)) {
            Headline(article, Brief.type.display, maxLines = 4)
            Spacer(Modifier.height(Space.s))
            Meta(article)
        }
    }
}

/** A full-width photo and big headline, the workhorse of the front page. */
@Composable
fun HeadlineCard(article: NewsArticle, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Brief.colors.card).story(onClick)) {
        if (article.format == StoryFormat.BREAKING) Collage(article) else ArticleArtwork(article)
        Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.m, bottom = Space.l)) {
            Headline(article, Brief.type.headline)
        }
    }
}

/** Two headlines side by side under square photos. */
@Composable
fun PairCards(left: NewsArticle, right: NewsArticle, onOpen: (NewsArticle) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Brief.colors.card)
        .padding(start = Space.gutter, end = Space.gutter, top = Space.gutter, bottom = Space.l),
        horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        listOf(left, right).forEach { article ->
            Column(Modifier.weight(1f).story { onOpen(article) }) {
                ArticleArtwork(article, ratio = Ratio.PAIR)
                Spacer(Modifier.height(Space.s))
                Headline(article, Brief.type.headlineSmall, maxLines = 4)
            }
        }
    }
}

/** Headline and deck on the left, a small picture on the right. Also used in lists and search. */
@Composable
fun RowCard(article: NewsArticle, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Brief.colors.card).story(onClick)) {
        Row(Modifier.padding(horizontal = Space.gutter, vertical = Space.gutter),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (article.format == StoryFormat.REVIEW || article.format == StoryFormat.BREAKING) {
                    Tag(article)
                    Spacer(Modifier.height(Space.xs))
                }
                Headline(article, Brief.type.rowTitle, maxLines = 3)
                Spacer(Modifier.height(Space.xs))
                Meta(article)
            }
            Spacer(Modifier.width(Space.m))
            ArticleArtwork(article, Modifier.width(112.dp).clip(BriefShapes.thumb), Ratio.THUMB)
        }
        Rule(Modifier.padding(horizontal = Space.gutter))
    }
}

@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(Space.hairline).background(Brief.colors.rule))
}

/** The parallelogram behind a ribbon's title. */
private val Slant = GenericShape { size, _ ->
    val lean = size.height * 0.45f
    moveTo(lean, 0f); lineTo(size.width, 0f); lineTo(size.width - lean, size.height); lineTo(0f, size.height); close()
}

/** A full-width bar that opens a section, with its title on a slanted block. */
@Composable
fun SectionRibbon(title: String, modifier: Modifier = Modifier, breaking: Boolean = false) {
    val c = Brief.colors
    Box(modifier.fillMaxWidth().padding(top = Space.m).background(if (breaking) c.breaking else c.masthead)
        .semantics { heading() }, contentAlignment = Alignment.Center) {
        Box(Modifier.background(if (breaking) c.masthead else c.ribbonLabel, Slant)
            .padding(horizontal = Space.xxl, vertical = Space.s)) {
            Text(title.uppercase(Locale.US), style = Brief.type.ribbon, color = c.onMasthead)
        }
    }
}

/** A columnist's portrait on a coloured blob, or their initials when there is no portrait. */
@Composable
fun Portrait(article: NewsArticle, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val c = Brief.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = -8f }.background(c.opinionAccent, BriefShapes.portraitBlob))
        val url = article.authorImageUrl
        if (url != null) {
            AsyncArticleImage(url, null, Modifier.fillMaxSize().padding(Space.xs), aspectRatio = 1f,
                shape = BriefShapes.round)
        } else {
            Text(initials(article.author ?: article.source), style = Brief.type.opinionHeadline, color = c.onImage)
        }
    }
}

private fun initials(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** A signed column: warm paper, portrait, serif headline. */
@Composable
fun OpinionCard(article: NewsArticle, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Brief.colors
    Column(modifier.fillMaxWidth().clip(BriefShapes.card).background(c.opinionPaper).story(onClick)
        .padding(Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Portrait(article, 64.dp)
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(article.author ?: article.source, style = Brief.type.rowTitle, color = c.ink, maxLines = scaledLines(1),
                    overflow = TextOverflow.Ellipsis)
                Text((article.authorRole ?: StoryFormat.OPINION.label).uppercase(Locale.US),
                    style = Brief.type.tag, color = c.opinionInk)
            }
        }
        Spacer(Modifier.height(Space.m))
        Text("“", style = Brief.type.opinionHeadline, color = c.opinionAccent,
            modifier = Modifier.offset(y = Space.xs).clearAndSetSemantics { })
        Text(article.title, style = Brief.type.opinionHeadline, color = c.ink, maxLines = scaledLines(4),
            overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s))
        Text(article.summary, style = Brief.type.quote, color = c.inkMuted, maxLines = scaledLines(2),
            overflow = TextOverflow.Ellipsis)
    }
}

/** Columns in a sideways rail; a single column fills the width. */
@Composable
fun OpinionRail(articles: List<NewsArticle>, onOpen: (NewsArticle) -> Unit) {
    if (articles.size == 1) {
        OpinionCard(articles.first(), Modifier.padding(Space.gutter)) { onOpen(articles.first()) }
        return
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.82f
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            articles.forEach { article ->
                OpinionCard(article, Modifier.width(cardWidth).heightIn(min = 280.dp)) { onOpen(article) }
            }
        }
    }
}

/** The AI Brief mark: a slanted block and the name. */
@Composable
fun BrandMark(color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 14.dp, height = 12.dp).background(Brief.colors.ribbonLabel, Slant))
        Spacer(Modifier.width(Space.xs))
        Text("AI Brief", style = Brief.type.tag, color = color)
    }
}

/**
 * A guide or sponsored post on its designed card: a named gradient, the brand
 * mark, a big title and the cover on the right fading into the colour.
 * Sponsored cards always say who paid, above the title.
 */
@Composable
fun DesignedCard(article: NewsArticle, modifier: Modifier = Modifier, onClick: (() -> Unit)?) {
    val palette = cardPalette(article.cardTheme)
    // In the feed the card teases the story; as the reader's header (no tap) it is
    // the story's own headline and standfirst, so nothing is cut short there.
    val titleLines = if (onClick != null) scaledLines(4) else Int.MAX_VALUE
    val summaryLines = if (onClick != null) scaledLines(3) else Int.MAX_VALUE
    // The card keeps its designed shape, but grows instead of clipping when large
    // system text needs more room, so the sponsor and title are always readable.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val designedHeight = maxWidth / DESIGNED_CARD_RATIO
        Box(Modifier.fillMaxWidth().heightIn(min = designedHeight).clip(BriefShapes.card).background(palette.brush)
            .then(if (onClick != null) Modifier.story(onClick) else Modifier)) {
            Box(Modifier.matchParentSize()) {
                // The picture melts into the gradient from the left and top instead of sitting on it as a box.
                ArticleArtwork(article, Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.6f)
                    .height(designedHeight * 0.64f)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.55f to Color.Black),
                            blendMode = BlendMode.DstIn)
                        drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color.Black),
                            blendMode = BlendMode.DstIn)
                    }, ratio = null)
            }
            Column(Modifier.fillMaxWidth().padding(Space.l)) {
                BrandMark(palette.ink)
                Spacer(Modifier.height(Space.l))
                if (article.format == StoryFormat.SPONSORED) {
                    SponsorPill(article)
                    Spacer(Modifier.height(Space.s))
                }
                Text(article.title, style = Brief.type.designedTitle, color = palette.ink, maxLines = titleLines,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.7f).semantics { heading() })
                Spacer(Modifier.height(Space.m))
                Text(article.summary, style = Brief.type.deck, color = palette.inkMuted, maxLines = summaryLines,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.52f))
            }
        }
    }
}

/** Width over height of a designed card at normal text size. */
private const val DESIGNED_CARD_RATIO = 1.18f

/** The disclosure every sponsored post carries. */
@Composable
fun SponsorPill(article: NewsArticle, modifier: Modifier = Modifier) {
    // Never cut short: the sponsor's name must stay readable at any text size.
    Text("SPONSORED · ${article.sponsor.orEmpty().uppercase(Locale.US)}", style = Brief.type.tag,
        color = Brief.colors.ink,
        modifier = modifier.background(Brief.colors.card, BriefShapes.pill)
            .padding(horizontal = Space.s, vertical = Space.xs))
}
