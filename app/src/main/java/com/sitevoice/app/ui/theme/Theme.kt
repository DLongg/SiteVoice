package com.sitevoice.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/** App theme. Light only (decision recorded in design-system/sitevoice/MASTER.md). */
@Composable
fun SiteVoiceTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSiteVoiceColors provides SiteVoiceLightExtendedColors) {
        MaterialTheme(
            colorScheme = SiteVoiceLightColorScheme,
            typography = SiteVoiceTypography,
            shapes = SiteVoiceShapes,
            content = content
        )
    }
}

/** Access point for SiteVoice-specific tokens: `SiteVoiceTheme.colors.success`, `SiteVoiceTheme.spacing.md`. */
object SiteVoiceTheme {
    val colors: SiteVoiceExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSiteVoiceColors.current

    val spacing = Spacing
}

/** 4dp grid (MASTER.md §3). */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Minimum touch target (Material 48dp). */
    val minTouchTarget = 48.dp
}
