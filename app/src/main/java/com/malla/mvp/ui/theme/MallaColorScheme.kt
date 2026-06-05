package com.malla.mvp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores completa para un tema de MALLA.
 * Todos los temas oscuros están optimizados para pantallas OLED
 * (fondos oscuros = píxeles apagados = ahorro de batería).
 */
data class MallaColorScheme(
    val name: String,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val outline: Color,
    val surfaceVariant: Color,
    val isDark: Boolean = true
) {
    companion object {
        // Tema 1: MALLA Oscuro (cyan + azul noche) — POR DEFECTO
        val MALLA_DARK = MallaColorScheme(
            name = "MALLA Oscuro",
            primary = Color(0xFF00B8D4),        // Cyan apagado
            onPrimary = Color(0xFF0A1B2A),
            primaryContainer = Color(0xFF004D5A),
            secondary = Color(0xFF4CAF50),
            onSecondary = Color(0xFF0A1B2A),
            secondaryContainer = Color(0xFF1B5E20),
            background = Color(0xFF0A1B2A),     // Azul noche profundo
            surface = Color(0xFF0F2233),
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0),
            outline = Color(0xFF00B8D4),
            surfaceVariant = Color(0xFF1A2C3E),
            isDark = true
        )

        // Tema 2: OLED Puro (máxima eficiencia energética) — FORZADO EN MODO MESH
        val OLED_PURE = MallaColorScheme(
            name = "OLED Puro",
            primary = Color(0xFF66BB6A),        // Verde apagado
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF81C784),
            onSecondary = Color(0xFF000000),
            secondaryContainer = Color(0xFF2E7D32),
            background = Color(0xFF000000),     // Negro total (OLED apagado)
            surface = Color(0xFF0A0A0A),
            onBackground = Color(0xFFCCCCCC),
            onSurface = Color(0xFFCCCCCC),
            outline = Color(0xFF66BB6A),
            surfaceVariant = Color(0xFF1A1A1A),
            isDark = true
        )

        // Tema 3: Midnight (azul profundo + gris)
        val MIDNIGHT = MallaColorScheme(
            name = "Midnight",
            primary = Color(0xFF5C6BC0),        // Índigo apagado
            onPrimary = Color(0xFF0D1117),
            primaryContainer = Color(0xFF1A237E),
            secondary = Color(0xFF78909C),
            onSecondary = Color(0xFF0D1117),
            secondaryContainer = Color(0xFF37474F),
            background = Color(0xFF0D1117),     // Casi negro azulado
            surface = Color(0xFF161B22),
            onBackground = Color(0xFFC8CCD0),
            onSurface = Color(0xFFC8CCD0),
            outline = Color(0xFF5C6BC0),
            surfaceVariant = Color(0xFF21262D),
            isDark = true
        )

        // Tema 4: Forest (verde bosque)
        val FOREST = MallaColorScheme(
            name = "Forest",
            primary = Color(0xFF4CAF50),        // Verde bosque
            onPrimary = Color(0xFF0D1A0D),
            primaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF8D6E63),
            onSecondary = Color(0xFF0D1A0D),
            secondaryContainer = Color(0xFF4E342E),
            background = Color(0xFF0D1A0D),
            surface = Color(0xFF152215),
            onBackground = Color(0xFFC8CCB8),
            onSurface = Color(0xFFC8CCB8),
            outline = Color(0xFF4CAF50),
            surfaceVariant = Color(0xFF1C2E1C),
            isDark = true
        )

        // Tema 5: Serenity (morado suave)
        val SERENITY = MallaColorScheme(
            name = "Serenity",
            primary = Color(0xFF9575CD),        // Morado suave
            onPrimary = Color(0xFF111118),
            primaryContainer = Color(0xFF4527A0),
            secondary = Color(0xFF80CBC4),
            onSecondary = Color(0xFF111118),
            secondaryContainer = Color(0xFF00695C),
            background = Color(0xFF111118),
            surface = Color(0xFF1A1A24),
            onBackground = Color(0xFFD0CCD8),
            onSurface = Color(0xFFD0CCD8),
            outline = Color(0xFF9575CD),
            surfaceVariant = Color(0xFF242230),
            isDark = true
        )

        // Tema 6: Sunset (naranja quemado)
        val SUNSET = MallaColorScheme(
            name = "Sunset",
            primary = Color(0xFFFF7043),        // Naranja quemado
            onPrimary = Color(0xFF1A1008),
            primaryContainer = Color(0xFFBF360C),
            secondary = Color(0xFFFFCA28),
            onSecondary = Color(0xFF1A1008),
            secondaryContainer = Color(0xFFF57F17),
            background = Color(0xFF1A1008),
            surface = Color(0xFF241A10),
            onBackground = Color(0xFFD8C8B8),
            onSurface = Color(0xFFD8C8B8),
            outline = Color(0xFFFF7043),
            surfaceVariant = Color(0xFF302218),
            isDark = true
        )

        // Tema 7: Claro (blanco hueso + gris)
        val LIGHT = MallaColorScheme(
            name = "Claro",
            primary = Color(0xFF00838F),        // Teal oscuro
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFB2EBF2),
            secondary = Color(0xFF43A047),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFC8E6C9),
            background = Color(0xFFF5F0EB),     // Blanco hueso
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1A1A1A),
            onSurface = Color(0xFF1A1A1A),
            outline = Color(0xFF00838F),
            surfaceVariant = Color(0xFFE8E0D8),
            isDark = false
        )

        // Lista de todos los temas disponibles
        val ALL = listOf(MALLA_DARK, OLED_PURE, MIDNIGHT, FOREST, SERENITY, SUNSET, LIGHT)
    }
}
