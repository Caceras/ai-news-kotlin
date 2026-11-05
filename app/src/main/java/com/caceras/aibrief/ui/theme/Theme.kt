package com.caceras.aibrief.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caceras.aibrief.R
import com.caceras.aibrief.data.CardTheme

/*
 * The AI Brief design system. This file is the only place in the app that may
 * define a colour, a font, a type size or a corner shape. Every screen reads
 * them through [Brief]. DesignRulesTest fails the build when code anywhere
 * else uses a raw colour, font size, font weight, corner shape or spacing
 * value. See docs/DESIGN_SYSTEM.md.
 */

/** Every colour role the app uses. Screens never pick a colour by value. */
@Immutable
data class BriefColors(
    /** The page behind cards. */
    val paper: Color,
    /** Cards, toolbars and sheets. */
    val card: Color,
    val ink: Color,
    val inkMuted: Color,
    /** Hairlines between stories. */
    val rule: Color,
    /** The masthead bar and section ribbons. */
    val masthead: Color,
    val onMasthead: Color,
    /** A text action on the masthead navy, such as a snackbar's Undo. Readable on navy in both themes. */
    val mastheadAction: Color,
    /** The second tone on a section ribbon's label block. */
    val ribbonLabel: Color,
    /** Links, selected tabs and the brand. */
    val accent: Color,
    val onAccent: Color,
    /** Breaking labels and kickers. Never used for anything else. */
    val breaking: Color,
    val onBreaking: Color,
    /** The warm paper an opinion column sits on, and its accent. */
    val opinionPaper: Color,
    val opinionAccent: Color,
    /** Placeholder behind an image while it loads. */
    val placeholder: Color,
    /** Text set on top of photographs and dark designed cards. */
    val onImage: Color,
    /** The fade that keeps text on photographs readable. */
    val imageScrim: Color,
)

private val LightBrief = BriefColors(
    paper = Color(0xFFF1F2F4),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF0B0D12),
    inkMuted = Color(0xFF5A606B),
    rule = Color(0xFFE2E4E8),
    masthead = Color(0xFF0A1F44),
    onMasthead = Color(0xFFFFFFFF),
    mastheadAction = Color(0xFF7FA5FF),
    ribbonLabel = Color(0xFF2F6BFF),
    accent = Color(0xFF1F55E0),
    onAccent = Color(0xFFFFFFFF),
    breaking = Color(0xFFD7262E),
    onBreaking = Color(0xFFFFFFFF),
    opinionPaper = Color(0xFFFBF1E6),
    opinionAccent = Color(0xFFE8833A),
    placeholder = Color(0xFFE4E7EC),
    onImage = Color(0xFFFFFFFF),
    imageScrim = Color(0xD90A0F1A),
)

private val DarkBrief = BriefColors(
    paper = Color(0xFF0B0D12),
    card = Color(0xFF151922),
    ink = Color(0xFFF2F3F5),
    inkMuted = Color(0xFF9AA1AD),
    rule = Color(0xFF262B36),
    masthead = Color(0xFF0A1F44),
    onMasthead = Color(0xFFFFFFFF),
    mastheadAction = Color(0xFF7FA5FF),
    ribbonLabel = Color(0xFF2F6BFF),
    accent = Color(0xFF7FA5FF),
    onAccent = Color(0xFF0B0D12),
    breaking = Color(0xFFFF4A52),
    onBreaking = Color(0xFFFFFFFF),
    opinionPaper = Color(0xFF241B14),
    opinionAccent = Color(0xFFF29A55),
    placeholder = Color(0xFF1E232D),
    onImage = Color(0xFFFFFFFF),
    imageScrim = Color(0xD9000000),
)

/** The background and type colour of a designed (guide or sponsored) card. */
@Immutable
data class CardPalette(
    val brush: Brush,
    /** The gradient's end colour, which pictures fade into. */
    val edge: Color,
    val ink: Color,
    val inkMuted: Color,
    val glow: Color,
)

private fun lightCard(start: Long, end: Long, glow: Long) = CardPalette(
    brush = Brush.linearGradient(listOf(Color(start), Color(end))), edge = Color(end),
    ink = Color(0xFF0B0D12), inkMuted = Color(0xFF3B414C), glow = Color(glow),
)

private fun darkCard(start: Long, end: Long, glow: Long) = CardPalette(
    brush = Brush.linearGradient(listOf(Color(start), Color(end))), edge = Color(end),
    ink = Color(0xFFFFFFFF), inkMuted = Color(0xFFD5DEEA), glow = Color(glow),
)

/** The eight card backgrounds. A post picks one by name and can never add a ninth. */
fun cardPalette(theme: CardTheme): CardPalette = when (theme) {
    CardTheme.SKY -> lightCard(0xFFF4FAFF, 0xFF8FDDF7, 0xFF2BB8E8)
    CardTheme.LILAC -> lightCard(0xFFF7F4FF, 0xFFC4ABF5, 0xFF8E63E6)
    CardTheme.MINT -> lightCard(0xFFF2FCF8, 0xFF8EE6C8, 0xFF25C495)
    CardTheme.PEACH -> lightCard(0xFFFFF6F0, 0xFFFAC59B, 0xFFF08A3C)
    CardTheme.BLUSH -> lightCard(0xFFFFF5FA, 0xFFF3B7D8, 0xFFE0609F)
    CardTheme.OCEAN -> darkCard(0xFF0D3B57, 0xFF1A6F8C, 0xFF3FD0E8)
    CardTheme.INDIGO -> darkCard(0xFF2A2F7A, 0xFF5B5BD6, 0xFF8FA2FF)
    CardTheme.NAVY -> darkCard(0xFF0A1F44, 0xFF1D4E8F, 0xFF4FA3FF)
}

private val Sans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
    Font(R.font.dm_sans_extrabold, FontWeight.ExtraBold),
)

private val Serif = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.newsreader_semibold, FontWeight.SemiBold),
)

/** Headlines sit tight on their line box, like a printed front page. */
private val Tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/**
 * The whole type ramp. Headlines are heavy sans; columns are serif; nothing
 * else exists. A screen picks a role, never a size.
 */
@Immutable
data class BriefType(
    /** The lead story's headline. */
    val display: TextStyle,
    /** A full-width headline card. */
    val headline: TextStyle,
    /** Two headlines side by side. */
    val headlineSmall: TextStyle,
    /** A compact story row. */
    val rowTitle: TextStyle,
    /** The one-line follow-up under a headline, set after a ➤. */
    val deck: TextStyle,
    /** Section ribbons, kickers on labels and tab names. */
    val label: TextStyle,
    /** Small caps labels on cards, such as the topic. */
    val tag: TextStyle,
    /** Dates, bylines and reading time. */
    val meta: TextStyle,
    val body: TextStyle,
    /** A heading inside an article. */
    val bodyHeading: TextStyle,
    /** The masthead wordmark. */
    val wordmark: TextStyle,
    /** Titles of the Saved and About pages. */
    val pageTitle: TextStyle,
    /** An opinion column's headline. */
    val opinionHeadline: TextStyle,
    /** An opinion column's body. */
    val opinionBody: TextStyle,
    /** Pull quotes and an opinion card's teaser. */
    val quote: TextStyle,
    /** A guide or sponsored card's title. */
    val designedTitle: TextStyle,
    val button: TextStyle,
    /** The slanted title on a section ribbon. */
    val ribbon: TextStyle,
)

private val BriefTypeScale = BriefType(
    display = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 31.sp,
        lineHeight = 34.sp, letterSpacing = (-0.8).sp, lineHeightStyle = Tight),
    headline = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp,
        lineHeight = 28.sp, letterSpacing = (-0.6).sp, lineHeightStyle = Tight),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp,
        lineHeight = 22.sp, letterSpacing = (-0.4).sp, lineHeightStyle = Tight),
    rowTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 17.sp,
        lineHeight = 21.sp, letterSpacing = (-0.2).sp),
    deck = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
        lineHeight = 16.sp, letterSpacing = 1.2.sp),
    tag = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp,
        lineHeight = 14.sp, letterSpacing = 1.0.sp),
    meta = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 17.sp,
        lineHeight = 27.sp, letterSpacing = 0.1.sp),
    bodyHeading = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp,
        lineHeight = 26.sp, letterSpacing = (-0.3).sp),
    wordmark = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp,
        lineHeight = 26.sp, letterSpacing = (-0.6).sp),
    pageTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 31.sp,
        lineHeight = 34.sp, letterSpacing = (-0.8).sp),
    opinionHeadline = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp,
        lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    opinionBody = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 19.sp,
        lineHeight = 30.sp),
    quote = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic,
        fontSize = 21.sp, lineHeight = 28.sp),
    designedTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 29.sp,
        lineHeight = 31.sp, letterSpacing = (-0.8).sp, lineHeightStyle = Tight),
    button = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    ribbon = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp,
        lineHeight = 22.sp, letterSpacing = 0.4.sp, textGeometricTransform = TextGeometricTransform(skewX = -0.22f)),
)

/** The spacing scale. Padding and gaps use these steps only. */
object Space {
    val hairline = 1.dp
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    /** The side margin of every screen. */
    val gutter = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val section = 40.dp
}

/** The only corner shapes. News photos are square-cornered; designed cards are round. */
object BriefShapes {
    val none: Shape = RoundedCornerShape(0.dp)
    val thumb: Shape = RoundedCornerShape(4.dp)
    val button: Shape = RoundedCornerShape(6.dp)
    val card: Shape = RoundedCornerShape(18.dp)
    val pill: Shape = RoundedCornerShape(50)
    val round: Shape = CircleShape
    /** The irregular backdrop behind a columnist's portrait. */
    val portraitBlob: Shape = RoundedCornerShape(topStartPercent = 46, topEndPercent = 38,
        bottomEndPercent = 52, bottomStartPercent = 34)
}

private val LocalBriefColors = staticCompositionLocalOf { LightBrief }
private val LocalBriefType = staticCompositionLocalOf { BriefTypeScale }

/** The design system, read inside any composable. */
object Brief {
    val colors: BriefColors
        @Composable @ReadOnlyComposable get() = LocalBriefColors.current
    val type: BriefType
        @Composable @ReadOnlyComposable get() = LocalBriefType.current
}

/**
 * Material components (text fields, progress, snackbars, the pull-to-refresh
 * spinner) take their colours from the same roles. Snackbars sit on the
 * masthead navy.
 */
private fun materialColors(c: BriefColors, dark: Boolean) = if (dark) {
    darkColorScheme(primary = c.accent, onPrimary = c.onAccent, background = c.paper, onBackground = c.ink,
        surface = c.card, onSurface = c.ink, surfaceVariant = c.placeholder, onSurfaceVariant = c.inkMuted,
        outline = c.inkMuted, outlineVariant = c.rule, error = c.breaking,
        inverseSurface = c.masthead, inverseOnSurface = c.onMasthead, inversePrimary = c.mastheadAction)
} else {
    lightColorScheme(primary = c.accent, onPrimary = c.onAccent, background = c.paper, onBackground = c.ink,
        surface = c.card, onSurface = c.ink, surfaceVariant = c.placeholder, onSurfaceVariant = c.inkMuted,
        outline = c.inkMuted, outlineVariant = c.rule, error = c.breaking,
        inverseSurface = c.masthead, inverseOnSurface = c.onMasthead, inversePrimary = c.mastheadAction)
}

private fun materialType(t: BriefType) = Typography(
    headlineLarge = t.pageTitle, headlineMedium = t.headline, titleLarge = t.bodyHeading,
    titleMedium = t.rowTitle, bodyLarge = t.body, bodyMedium = t.deck, labelLarge = t.button,
    labelMedium = t.meta, labelSmall = t.meta,
)

@Composable
fun AiBriefTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkBrief else LightBrief
    CompositionLocalProvider(LocalBriefColors provides colors, LocalBriefType provides BriefTypeScale) {
        MaterialTheme(colorScheme = materialColors(colors, dark), typography = materialType(BriefTypeScale),
            content = content)
    }
}
