package com.iperf3.android.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Light scheme core colors ----------
val md_theme_light_primary = Color(0xFF1976D2)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
val md_theme_light_onPrimaryContainer = Color(0xFF001D36)

val md_theme_light_secondary = Color(0xFF26A69A)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFA7F3EC)
val md_theme_light_onSecondaryContainer = Color(0xFF00201D)

val md_theme_light_tertiary = Color(0xFF1565C0)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFD6E3FF)
val md_theme_light_onTertiaryContainer = Color(0xFF001B3E)

val md_theme_light_error = Color(0xFFD32F2F)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)

val md_theme_light_background = Color(0xFFFAFAFA)
val md_theme_light_onBackground = Color(0xFF212121)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF212121)
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)
val md_theme_light_outline = Color(0xFF79747E)
val md_theme_light_outlineVariant = Color(0xFFCAC4D0)
val md_theme_light_inverseSurface = Color(0xFF313033)
val md_theme_light_inverseOnSurface = Color(0xFFF4EFF4)
val md_theme_light_inversePrimary = Color(0xFF9ECAFF)
val md_theme_light_surfaceTint = Color(0xFF1976D2)
val md_theme_light_scrim = Color(0xFF000000)

// ---------- Dark scheme core colors ----------
val md_theme_dark_primary = Color(0xFF9ECAFF)
val md_theme_dark_onPrimary = Color(0xFF003258)
val md_theme_dark_primaryContainer = Color(0xFF0D5FAA)
val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)

val md_theme_dark_secondary = Color(0xFF80CBC4)
val md_theme_dark_onSecondary = Color(0xFF003733)
val md_theme_dark_secondaryContainer = Color(0xFF00695C)
val md_theme_dark_onSecondaryContainer = Color(0xFFA7F3EC)

val md_theme_dark_tertiary = Color(0xFFAAC7FF)
val md_theme_dark_onTertiary = Color(0xFF002F65)
val md_theme_dark_tertiaryContainer = Color(0xFF0E4D97)
val md_theme_dark_onTertiaryContainer = Color(0xFFD6E3FF)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_dark_background = Color(0xFF1C1B1F)
val md_theme_dark_onBackground = Color(0xFFE6E1E5)
val md_theme_dark_surface = Color(0xFF1C1B1F)
val md_theme_dark_onSurface = Color(0xFFE6E1E5)
val md_theme_dark_surfaceVariant = Color(0xFF49454F)
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0)
val md_theme_dark_outline = Color(0xFF938F99)
val md_theme_dark_outlineVariant = Color(0xFF49454F)
val md_theme_dark_inverseSurface = Color(0xFFE6E1E5)
val md_theme_dark_inverseOnSurface = Color(0xFF313033)
val md_theme_dark_inversePrimary = Color(0xFF1976D2)
val md_theme_dark_surfaceTint = Color(0xFF9ECAFF)
val md_theme_dark_scrim = Color(0xFF000000)

// ---------- Quality score colors ----------
object QualityColors {
    val Excellent = Color(0xFF4CAF50)
    val Good = Color(0xFF8BC34A)
    val Fair = Color(0xFFFFC107)
    val Poor = Color(0xFFFF9800)
    val Bad = Color(0xFFF44336)
}

object QualityColorsDark {
    val Excellent = Color(0xFF81C784)
    val Good = Color(0xFFAED581)
    val Fair = Color(0xFFFFD54F)
    val Poor = Color(0xFFFFB74D)
    val Bad = Color(0xFFE57373)
}

// ---------- Chart colors ----------
object ChartColors {
    val Bandwidth = Color(0xFF2196F3)
    val Jitter = Color(0xFFFF9800)
    val Loss = Color(0xFFF44336)
    val Grid = Color(0xFFE0E0E0)
}

object ChartColorsDark {
    val Bandwidth = Color(0xFF64B5F6)
    val Jitter = Color(0xFFFFB74D)
    val Loss = Color(0xFFE57373)
    val Grid = Color(0xFF424242)
}

// ---------- Status colors ----------
object StatusColors {
    val Running = Color(0xFF4CAF50)
    val Stopped = Color(0xFF9E9E9E)
    val Error = Color(0xFFF44336)
}

object StatusColorsDark {
    val Running = Color(0xFF81C784)
    val Stopped = Color(0xFFBDBDBD)
    val Error = Color(0xFFE57373)
}
