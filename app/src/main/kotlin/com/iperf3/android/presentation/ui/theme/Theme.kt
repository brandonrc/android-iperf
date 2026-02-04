package com.iperf3.android.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---------- Material3 color schemes ----------

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    scrim = md_theme_light_scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    scrim = md_theme_dark_scrim,
)

// ---------- Extended color sets ----------

@Immutable
data class IPerf3QualityColors(
    val excellent: Color = Color.Unspecified,
    val good: Color = Color.Unspecified,
    val fair: Color = Color.Unspecified,
    val poor: Color = Color.Unspecified,
    val bad: Color = Color.Unspecified,
)

@Immutable
data class IPerf3ChartColors(
    val bandwidth: Color = Color.Unspecified,
    val jitter: Color = Color.Unspecified,
    val loss: Color = Color.Unspecified,
    val grid: Color = Color.Unspecified,
)

@Immutable
data class IPerf3StatusColors(
    val running: Color = Color.Unspecified,
    val stopped: Color = Color.Unspecified,
    val error: Color = Color.Unspecified,
)

@Immutable
data class IPerf3ExtendedColors(
    val quality: IPerf3QualityColors = IPerf3QualityColors(),
    val chart: IPerf3ChartColors = IPerf3ChartColors(),
    val status: IPerf3StatusColors = IPerf3StatusColors(),
)

// ---------- CompositionLocal ----------

val LocalIPerf3Colors = staticCompositionLocalOf { IPerf3ExtendedColors() }

// ---------- Pre-built instances ----------

private val LightExtendedColors = IPerf3ExtendedColors(
    quality = IPerf3QualityColors(
        excellent = QualityColors.Excellent,
        good = QualityColors.Good,
        fair = QualityColors.Fair,
        poor = QualityColors.Poor,
        bad = QualityColors.Bad,
    ),
    chart = IPerf3ChartColors(
        bandwidth = ChartColors.Bandwidth,
        jitter = ChartColors.Jitter,
        loss = ChartColors.Loss,
        grid = ChartColors.Grid,
    ),
    status = IPerf3StatusColors(
        running = StatusColors.Running,
        stopped = StatusColors.Stopped,
        error = StatusColors.Error,
    ),
)

private val DarkExtendedColors = IPerf3ExtendedColors(
    quality = IPerf3QualityColors(
        excellent = QualityColorsDark.Excellent,
        good = QualityColorsDark.Good,
        fair = QualityColorsDark.Fair,
        poor = QualityColorsDark.Poor,
        bad = QualityColorsDark.Bad,
    ),
    chart = IPerf3ChartColors(
        bandwidth = ChartColorsDark.Bandwidth,
        jitter = ChartColorsDark.Jitter,
        loss = ChartColorsDark.Loss,
        grid = ChartColorsDark.Grid,
    ),
    status = IPerf3StatusColors(
        running = StatusColorsDark.Running,
        stopped = StatusColorsDark.Stopped,
        error = StatusColorsDark.Error,
    ),
)

// ---------- Theme composable ----------

@Composable
fun IPerf3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIPerf3Colors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IPerf3Typography,
            content = content,
        )
    }
}

// ---------- Convenience accessor ----------

object IPerf3Theme {
    val extendedColors: IPerf3ExtendedColors
        @Composable
        get() = LocalIPerf3Colors.current
}
