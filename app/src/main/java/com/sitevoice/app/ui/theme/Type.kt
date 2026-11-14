package com.sitevoice.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Type scale: design-system/sitevoice/MASTER.md §2. System font, 5 sizes (28/20/16/14/12), min 12sp.

private val Base = TextStyle(fontFamily = FontFamily.Default)

private val Display = Base.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
private val Title = Base.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
private val Subtitle = Base.copy(fontSize = 16.sp, lineHeight = 24.sp)
private val Body = Base.copy(fontSize = 14.sp, lineHeight = 20.sp)
private val Caption = Base.copy(fontSize = 12.sp, lineHeight = 16.sp)

internal val SiteVoiceTypography = Typography(
    displayLarge = Display,
    displayMedium = Display,
    displaySmall = Display,
    headlineLarge = Title,
    headlineMedium = Title,
    headlineSmall = Title,
    titleLarge = Title,
    titleMedium = Subtitle.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Body.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Subtitle,
    bodyMedium = Body,
    bodySmall = Caption,
    labelLarge = Body.copy(fontWeight = FontWeight.Medium),
    labelMedium = Caption.copy(fontWeight = FontWeight.Medium),
    labelSmall = Caption.copy(fontWeight = FontWeight.Medium)
)
