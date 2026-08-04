package com.macsense.ai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Macsense ships one authored brand identity (dark, purple/gold) and does not
// opt into Material You dynamic color, so the Ari/Vinny Mac look is consistent
// across every device rather than tinted by wallpaper.
private val MacsenseDarkColorScheme = darkColorScheme(
    primary = MacsenseGoldPrimary,
    onPrimary = MacsenseVoidBlack,
    secondary = MacsenseAccentPurple,
    onSecondary = MacsenseTextPrimary,
    tertiary = MacsenseAccentPurpleBright,
    background = MacsenseVoidBlack,
    onBackground = MacsenseTextPrimary,
    surface = MacsensePanelPurple,
    onSurface = MacsenseTextPrimary,
    surfaceVariant = MacsenseCardPurple,
    onSurfaceVariant = MacsenseTextSecondary,
    outline = MacsenseBorderPurple,
    error = MacsenseError,
)

private val MacsenseLightColorScheme = lightColorScheme(
    primary = MacsenseAccentPurple,
    onPrimary = MacsenseTextPrimary,
    secondary = MacsenseGoldPrimary,
    onSecondary = MacsenseVoidBlack,
    background = Purple80,
    onBackground = MacsenseVoidBlack,
    surface = Purple80,
    onSurface = MacsenseVoidBlack,
)

@Composable
fun MacSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color deliberately defaults to false: Macsense's dark
    // purple/gold brand identity is fixed, not derived from wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MacsenseDarkColorScheme
        else -> MacsenseLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
