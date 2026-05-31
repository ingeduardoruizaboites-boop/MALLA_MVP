package com.malla.mvp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val LocalFontScale = staticCompositionLocalOf { 1.0f }

@Composable
fun MallaTheme(
    primaryColor: Color = Color(0xFF26C6DA),
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val onPrimary = if (primaryColor.luminance() > 0.5) Color.Black else Color.White
    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = onPrimary,
        primaryContainer = primaryColor.copy(alpha = 0.15f),
        secondary = Color(0xFF66BB6A),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF66BB6A).copy(alpha = 0.15f),
        background = Color(0xFF0A1B2A),
        surface = Color(0xFF0A1B2A),
        onBackground = Color(0xFFE0E0E0),
        onSurface = Color(0xFFE0E0E0),
        outline = primaryColor,
        surfaceVariant = Color(0xFF1A2C3E)
    )

    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = colorScheme,
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

fun Color.luminance(): Float {
    val r = this.red
    val g = this.green
    val b = this.blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
