package com.anothersshclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// High-contrast greyscale tuned for Daylight Live Paper.
private val PaperWhite = Color(0xFFFFFFFF)
private val InkBlack = Color(0xFF000000)
private val SoftGray = Color(0xFFEEEEEE)
private val MidGray = Color(0xFF666666)
private val DarkSurface = Color(0xFF111111)
private val DarkSoftGray = Color(0xFF2A2A2A)

private val PaperLightColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = PaperWhite,
    secondary = InkBlack,
    onSecondary = PaperWhite,
    tertiary = MidGray,
    onTertiary = PaperWhite,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = SoftGray,
    onSurfaceVariant = InkBlack,
    outline = InkBlack,
    outlineVariant = MidGray,
    error = InkBlack,
    onError = PaperWhite,
)

private val PaperDarkColorScheme = darkColorScheme(
    primary = PaperWhite,
    onPrimary = InkBlack,
    secondary = PaperWhite,
    onSecondary = InkBlack,
    tertiary = MidGray,
    onTertiary = PaperWhite,
    background = InkBlack,
    onBackground = PaperWhite,
    surface = DarkSurface,
    onSurface = PaperWhite,
    surfaceVariant = DarkSoftGray,
    onSurfaceVariant = PaperWhite,
    outline = PaperWhite,
    outlineVariant = MidGray,
    error = PaperWhite,
    onError = InkBlack,
)

private val PaperTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

val LocalTerminalTheme = staticCompositionLocalOf { TerminalTheme.Light }

@Composable
fun AnotherSshClientTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val terminalTheme = if (darkTheme) TerminalTheme.Dark else TerminalTheme.Light

    CompositionLocalProvider(LocalTerminalTheme provides terminalTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) PaperDarkColorScheme else PaperLightColorScheme,
            typography = PaperTypography,
            content = content,
        )
    }
}
