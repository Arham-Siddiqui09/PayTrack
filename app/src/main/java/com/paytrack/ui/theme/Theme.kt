package com.paytrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    secondary = AppBlue,
    tertiary = AppCoral,
    background = AppBackgroundDark,
    surface = AppSurfaceDark,
    onPrimary = AppTextPrimary,
    onSecondary = AppTextPrimary,
    onTertiary = AppTextPrimary,
    onBackground = AppTextPrimaryDark,
    onSurface = AppTextPrimaryDark,
    onSurfaceVariant = AppTextSecondaryDark,
    outline = AppBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    secondary = AppBlue,
    tertiary = AppCoral,
    background = AppBackground,
    surface = AppSurface,
    surfaceVariant = AppSurfaceMuted,
    onPrimary = AppTextPrimary,
    onSecondary = AppTextPrimary,
    onTertiary = AppTextPrimary,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary,
    onSurfaceVariant = AppTextSecondary,
    outline = AppBorder
)

@Composable
fun PayTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
