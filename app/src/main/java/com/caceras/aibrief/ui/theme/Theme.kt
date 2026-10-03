package com.caceras.aibrief.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.caceras.aibrief.R

private val PureBlack = Color(0xFF192923)
private val PureWhite = Color(0xFFFFFFFF)
private val LightMuted = Color(0xFF6C7169)
private val LightDivider = Color(0xFFE0E2DB)

private val DarkBackground = Color(0xFF0E0E0D)
private val DarkText = Color(0xFFEDEDE8)
private val DarkMuted = Color(0xFF8E8D88)
private val DarkDivider = Color(0xFF222220)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2D5646),
    onPrimary = PureWhite,
    background = Color(0xFFF4F5F0),
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = Color(0xFFE6ECE5),
    onSurfaceVariant = LightMuted,
    outline = Color(0xFFDCDCD8),
    outlineVariant = LightDivider,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8D1BC),
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkText,
    surface = Color(0xFF191D19),
    onSurface = DarkText,
    surfaceVariant = Color(0xFF181816),
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF333330),
    outlineVariant = DarkDivider,
)

private val EditorialSerif = FontFamily(Font(R.font.newsreader_regular))
private val EditorialSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
)

private val AppTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = EditorialSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.5.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = EditorialSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun AiBriefTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
