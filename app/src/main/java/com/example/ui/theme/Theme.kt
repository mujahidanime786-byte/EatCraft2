package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CraftEatsLightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = WhiteBg,
    primaryContainer = RoyalBlueSoft,
    onPrimaryContainer = RoyalBlueDark,
    secondary = DarkNavy,
    onSecondary = WhiteBg,
    secondaryContainer = SecondaryLavenderBg,
    onSecondaryContainer = MainText,
    tertiary = SuccessGreen,
    onTertiary = WhiteBg,
    background = WhiteBg,
    onBackground = MainText,
    surface = WhiteBg,
    onSurface = MainText,
    surfaceVariant = SecondaryLavenderBg,
    onSurfaceVariant = SecondaryText,
    outline = BorderGray,
    outlineVariant = BorderGray,
    error = ErrorRed,
    onError = WhiteBg
)

val CraftEatsShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun CraftEatsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CraftEatsLightColorScheme,
        typography = CraftEatsTypography,
        shapes = CraftEatsShapes,
        content = content
    )
}
