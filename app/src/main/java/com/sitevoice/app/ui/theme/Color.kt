package com.sitevoice.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Token values: design-system/sitevoice/MASTER.md §1. Light theme only.

internal val Blue600 = Color(0xFF2563EB)
internal val Blue700 = Color(0xFF1D4ED8)
internal val Blue800 = Color(0xFF1E40AF)
internal val Blue50 = Color(0xFFEFF6FF)

internal val Slate900 = Color(0xFF0F172A)
internal val Slate600 = Color(0xFF475569)
internal val Slate500 = Color(0xFF64748B)
internal val Slate200 = Color(0xFFE2E8F0)
internal val Slate100 = Color(0xFFF1F5F9)
internal val Slate50 = Color(0xFFF8FAFC)

internal val Red600 = Color(0xFFDC2626)
internal val Red700 = Color(0xFFB91C1C)
internal val Red50 = Color(0xFFFEF2F2)

internal val Green700 = Color(0xFF15803D)
internal val Green800 = Color(0xFF166534)
internal val Green100 = Color(0xFFDCFCE7)

internal val Amber700 = Color(0xFFB45309)
internal val Amber800 = Color(0xFF92400E)
internal val Amber100 = Color(0xFFFEF3C7)

internal val SiteVoiceLightColorScheme: ColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue800,
    inversePrimary = Blue700,
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate900,
    tertiary = Blue700,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceTint = Color.Transparent,
    surfaceBright = Color.White,
    surfaceDim = Slate100,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Slate50,
    surfaceContainerHigh = Slate100,
    surfaceContainerHighest = Slate100,
    outline = Slate500,
    outlineVariant = Slate200,
    error = Red600,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red700,
    scrim = Color(0x99000000)
)

/** Semantic colors Material3 does not provide (MASTER.md §1). */
@Immutable
data class SiteVoiceExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

internal val SiteVoiceLightExtendedColors = SiteVoiceExtendedColors(
    success = Green700,
    onSuccess = Color.White,
    successContainer = Green100,
    onSuccessContainer = Green800,
    warning = Amber700,
    onWarning = Color.White,
    warningContainer = Amber100,
    onWarningContainer = Amber800
)

val LocalSiteVoiceColors = staticCompositionLocalOf { SiteVoiceLightExtendedColors }
