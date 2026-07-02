package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val VeloceDarkColorScheme = darkColorScheme(
    primary = VelocePrimary,
    onPrimary = VeloceOnPrimary,
    primaryContainer = VelocePrimaryContainer,
    secondary = VeloceSecondary,
    onSecondary = VeloceOnSecondary,
    secondaryContainer = VeloceSecondaryContainer,
    tertiary = VeloceTertiary,
    background = VeloceDarkBackground,
    surface = VeloceDarkSurface,
    surfaceVariant = VeloceDarkSurfaceCard,
    onBackground = VeloceOnBackground,
    onSurface = VeloceOnSurface,
    onSurfaceVariant = VeloceOnSurfaceMuted
)

private val VeloceLightColorScheme = lightColorScheme(
    primary = VelocePrimary,
    onPrimary = VeloceOnPrimary,
    primaryContainer = VelocePrimaryContainer,
    secondary = VeloceSecondary,
    onSecondary = VeloceOnSecondary,
    secondaryContainer = VeloceSecondaryContainer,
    tertiary = VeloceTertiary,
    background = VeloceLightBackground,
    surface = VeloceLightSurface,
    surfaceVariant = VeloceLightSurfaceCard,
    onBackground = VeloceLightOnBackground,
    onSurface = VeloceLightOnSurface,
    onSurfaceVariant = VeloceLightOnSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default to false for custom brand-integrity, but support if desired
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> VeloceDarkColorScheme
        else -> VeloceLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
