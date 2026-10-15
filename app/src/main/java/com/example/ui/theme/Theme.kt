package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TvDarkColorScheme = darkColorScheme(
    primary = TvBrandBlue,
    onPrimary = TvTextWhite,
    primaryContainer = TvSurfaceVariant,
    onPrimaryContainer = TvTextPrimary,
    secondary = TvBullGreen,
    onSecondary = TvTextWhite,
    secondaryContainer = TvBullGreenBg,
    onSecondaryContainer = TvBullGreenBright,
    tertiary = TvAlertAmber,
    onTertiary = TvTextWhite,
    background = TvBackground,
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    outline = TvBorder,
    outlineVariant = TvDivider,
    error = TvBearRed,
    onError = TvTextWhite
)

private val TvLightColorScheme = lightColorScheme(
    primary = TvBrandBlue,
    onPrimary = TvTextWhite,
    primaryContainer = TvSurfaceVariant,
    onPrimaryContainer = TvTextPrimary,
    secondary = TvBullGreen,
    onSecondary = TvTextWhite,
    background = TvBackground, // Keeping authentic TradingView dark canvas
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    outline = TvBorder,
    outlineVariant = TvDivider,
    error = TvBearRed,
    onError = TvTextWhite
)

@Composable
fun TradeAlertTheme(
    darkTheme: Boolean = true, // Default to TradingView's signature pro dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = TvDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = TvBackground.toArgb()
                it.navigationBarColor = TvBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
