package com.malla.mvp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.sp

val LocalFontScale = staticCompositionLocalOf { 1.0f }
val LocalColorScheme = staticCompositionLocalOf { MallaColorScheme.MALLA_DARK }

@Composable
fun MallaTheme(
    colorScheme: MallaColorScheme = MallaColorScheme.MALLA_DARK,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val materialScheme = if (colorScheme.isDark) {
        darkColorScheme(
            primary = colorScheme.primary,
            onPrimary = colorScheme.onPrimary,
            primaryContainer = colorScheme.primaryContainer,
            secondary = colorScheme.secondary,
            onSecondary = colorScheme.onSecondary,
            secondaryContainer = colorScheme.secondaryContainer,
            background = colorScheme.background,
            surface = colorScheme.surface,
            onBackground = colorScheme.onBackground,
            onSurface = colorScheme.onSurface,
            outline = colorScheme.outline,
            surfaceVariant = colorScheme.surfaceVariant
        )
    } else {
        lightColorScheme(
            primary = colorScheme.primary,
            onPrimary = colorScheme.onPrimary,
            primaryContainer = colorScheme.primaryContainer,
            secondary = colorScheme.secondary,
            onSecondary = colorScheme.onSecondary,
            secondaryContainer = colorScheme.secondaryContainer,
            background = colorScheme.background,
            surface = colorScheme.surface,
            onBackground = colorScheme.onBackground,
            onSurface = colorScheme.onSurface,
            outline = colorScheme.outline,
            surfaceVariant = colorScheme.surfaceVariant
        )
    }

    CompositionLocalProvider(
        LocalFontScale provides fontScale,
        LocalColorScheme provides colorScheme
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = MaterialTheme.typography.copy(
                bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp * fontScale),
                bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp * fontScale),
                bodySmall = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp * fontScale),
                titleMedium = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp * fontScale),
                headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp * fontScale),
                labelSmall = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp * fontScale)
            ),
            content = content
        )
    }
}
