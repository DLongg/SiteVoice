package com.sitevoice.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Type scale: design-system/sitevoice/MASTER.md §2. System font, 5 sizes (28/20/16/14/12), min 12sp.

/** Raw size tokens, for call sites that set fontSize/lineHeight directly instead of a full style. */
object TextSizes {
    val display = 28.sp
    val title = 20.sp
    val subtitle = 16.sp
    val body = 14.sp
    val caption = 12.sp

    val displayLine = 36.sp
    val titleLine = 28.sp
    val subtitleLine = 24.sp
    val bodyLine = 20.sp
    val captionLine = 16.sp
}

private val Base = TextStyle(fontFamily = FontFamily.Default)

private val Display = Base.copy(fontSize = TextSizes.display, lineHeight = TextSizes.displayLine, fontWeight = FontWeight.SemiBold)
private val Title = Base.copy(fontSize = TextSizes.title, lineHeight = TextSizes.titleLine, fontWeight = FontWeight.SemiBold)
private val Subtitle = Base.copy(fontSize = TextSizes.subtitle, lineHeight = TextSizes.subtitleLine)
private val Body = Base.copy(fontSize = TextSizes.body, lineHeight = TextSizes.bodyLine)
private val Caption = Base.copy(fontSize = TextSizes.caption, lineHeight = TextSizes.captionLine)

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
