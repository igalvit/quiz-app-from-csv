package es.igalvit.quizappfromcsv.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Enhanced dark color scheme with additional semantic colors
private val EnhancedDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,

    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,

    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = OutlineDark,

    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceDimDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerHigh = SurfaceContainerHighDark
)

// Enhanced light color scheme with additional semantic colors
private val EnhancedLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,

    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,

    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,

    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    outline = OutlineLight,

    surfaceBright = SurfaceBrightLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainerHigh = SurfaceContainerHighLight
)

// Legacy color schemes (keeping for backward compatibility)
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Determines if a color needs dark text based on its luminance
 */
fun Color.requiresDarkText(): Boolean = luminance() > 0.5f

/**
 * Enhanced theme that automatically follows the Android system theme setting
 * with smooth transitions and improved system UI integration
 */
@Composable
fun QuizAppFromCSVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    enhancedTheme: Boolean = true, // Set to true by default to use enhanced theme
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (systemInDarkTheme)
                dynamicDarkColorScheme(context)
            else
                dynamicLightColorScheme(context)

            // Enhance dynamic color scheme with additional elevation tones if needed
            enhanceDynamicColorScheme(dynamicScheme, systemInDarkTheme)
        }
        enhancedTheme && systemInDarkTheme -> EnhancedDarkColorScheme
        enhancedTheme -> EnhancedLightColorScheme
        systemInDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Animate color transitions for smoother theme changes when system theme changes
    val animatedColorScheme = animateColorScheme(colorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Set navigation bar to be slightly translucent with theme color
            val navigationBarColor = colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
            window.navigationBarColor = navigationBarColor.toArgb()

            // Update status bar color
            window.statusBarColor = Color.Transparent.toArgb()

            // Make system bars content color adaptive to our theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !systemInDarkTheme
                isAppearanceLightNavigationBars = !systemInDarkTheme
            }

            // Apply a scrim for better readability when needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    // Listen for changes to system dark theme setting
    DisposableEffect(systemInDarkTheme) {
        onDispose { }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Enhances dynamic color schemes with additional elevation tones
 */
private fun enhanceDynamicColorScheme(scheme: ColorScheme, isDark: Boolean): ColorScheme {
    // Create additional surface tones that might be missing from dynamic schemes
    return scheme.copy(
        surfaceBright = if (isDark) {
            scheme.surface.copy(alpha = 1.1f) // Slightly lighter than base surface
        } else {
            scheme.surface.copy(alpha = 1.05f) // Slightly brighter than base surface
        },
        surfaceDim = if (isDark) {
            scheme.surface.copy(alpha = 0.9f) // Slightly darker than base surface
        } else {
            scheme.surface.copy(alpha = 0.95f) // Slightly dimmer than base surface
        },
        surfaceContainerLow = if (isDark) {
            scheme.surface.copy(alpha = 0.95f)
        } else {
            scheme.surface.copy(alpha = 1.01f)
        },
        surfaceContainerHigh = if (isDark) {
            scheme.surface.copy(alpha = 1.15f)
        } else {
            scheme.surface.copy(alpha = 1.08f)
        }
    )
}

/**
 * Animates color transitions for smoother theme changes
 */
@Composable
private fun animateColorScheme(colorScheme: ColorScheme): ColorScheme {
    val animationSpec = spring<Color>(stiffness = Spring.StiffnessMedium)

    return ColorScheme(
        primary = animateColorAsState(colorScheme.primary, animationSpec).value,
        onPrimary = animateColorAsState(colorScheme.onPrimary, animationSpec).value,
        primaryContainer = animateColorAsState(colorScheme.primaryContainer, animationSpec).value,
        onPrimaryContainer = animateColorAsState(colorScheme.onPrimaryContainer, animationSpec).value,
        inversePrimary = animateColorAsState(colorScheme.inversePrimary, animationSpec).value,

        secondary = animateColorAsState(colorScheme.secondary, animationSpec).value,
        onSecondary = animateColorAsState(colorScheme.onSecondary, animationSpec).value,
        secondaryContainer = animateColorAsState(colorScheme.secondaryContainer, animationSpec).value,
        onSecondaryContainer = animateColorAsState(colorScheme.onSecondaryContainer, animationSpec).value,

        tertiary = animateColorAsState(colorScheme.tertiary, animationSpec).value,
        onTertiary = animateColorAsState(colorScheme.onTertiary, animationSpec).value,
        tertiaryContainer = animateColorAsState(colorScheme.tertiaryContainer, animationSpec).value,
        onTertiaryContainer = animateColorAsState(colorScheme.onTertiaryContainer, animationSpec).value,

        error = animateColorAsState(colorScheme.error, animationSpec).value,
        onError = animateColorAsState(colorScheme.onError, animationSpec).value,
        errorContainer = animateColorAsState(colorScheme.errorContainer, animationSpec).value,
        onErrorContainer = animateColorAsState(colorScheme.onErrorContainer, animationSpec).value,

        background = animateColorAsState(colorScheme.background, animationSpec).value,
        onBackground = animateColorAsState(colorScheme.onBackground, animationSpec).value,

        surface = animateColorAsState(colorScheme.surface, animationSpec).value,
        onSurface = animateColorAsState(colorScheme.onSurface, animationSpec).value,
        surfaceVariant = animateColorAsState(colorScheme.surfaceVariant, animationSpec).value,
        onSurfaceVariant = animateColorAsState(colorScheme.onSurfaceVariant, animationSpec).value,
        surfaceTint = animateColorAsState(colorScheme.surfaceTint, animationSpec).value,

        inverseSurface = animateColorAsState(colorScheme.inverseSurface, animationSpec).value,
        inverseOnSurface = animateColorAsState(colorScheme.inverseOnSurface, animationSpec).value,

        outline = animateColorAsState(colorScheme.outline, animationSpec).value,
        outlineVariant = animateColorAsState(colorScheme.outlineVariant, animationSpec).value,
        scrim = animateColorAsState(colorScheme.scrim, animationSpec).value,

        // Additional surface tones for our enhanced theme
        surfaceBright = animateColorAsState(colorScheme.surfaceBright, animationSpec).value,
        surfaceDim = animateColorAsState(colorScheme.surfaceDim, animationSpec).value,
        surfaceContainer = animateColorAsState(colorScheme.surfaceContainer, animationSpec).value,
        surfaceContainerHigh = animateColorAsState(colorScheme.surfaceContainerHigh, animationSpec).value,
        surfaceContainerHighest = animateColorAsState(colorScheme.surfaceContainerHighest, animationSpec).value,
        surfaceContainerLow = animateColorAsState(colorScheme.surfaceContainerLow, animationSpec).value,
        surfaceContainerLowest = animateColorAsState(colorScheme.surfaceContainerLowest, animationSpec).value
    )
}
