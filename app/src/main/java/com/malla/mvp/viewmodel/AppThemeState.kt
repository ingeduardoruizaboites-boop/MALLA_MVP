package com.malla.mvp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ThemeColors(
    val primary: Color,
    val name: String
)

class AppThemeState private constructor(private val prefs: SharedPreferences?) {

    companion object {
        private const val DEFAULT_COLOR = 0xFF1976D2
        private const val DEFAULT_NAME = "Azul"

        fun create(context: Context): AppThemeState {
            val prefs = try {
                context.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                Log.e("AppThemeState", "Error accediendo a SharedPreferences", e)
                null
            }
            return AppThemeState(prefs)
        }

        fun createFallback(): AppThemeState = AppThemeState(null)

        val availableThemes = listOf(
            ThemeColors(Color(0xFF1976D2), "Azul"),
            ThemeColors(Color(0xFFD32F2F), "Rojo"),
            ThemeColors(Color(0xFFE91E63), "Rosa"),
            ThemeColors(Color(0xFF9C27B0), "Morado"),
            ThemeColors(Color(0xFF757575), "Gris"),
            ThemeColors(Color(0xFFFFFFFF), "Blanco"),
        )
    }

    private val _currentTheme = MutableStateFlow(loadTheme())
    val currentTheme: StateFlow<ThemeColors> = _currentTheme

    fun selectTheme(colors: ThemeColors) {
        _currentTheme.value = colors
        saveTheme(colors)
    }

    private fun loadTheme(): ThemeColors {
        val colorName = prefs?.getString("theme_name", DEFAULT_NAME) ?: DEFAULT_NAME
        val colorValue = prefs?.getLong("theme_color", DEFAULT_COLOR) ?: DEFAULT_COLOR
        return ThemeColors(
            primary = Color(colorValue.toULong()),
            name = colorName
        )
    }

    private fun saveTheme(colors: ThemeColors) {
        try {
            prefs?.edit()
                ?.putString("theme_name", colors.name)
                ?.putLong("theme_color", colors.primary.value.toLong())
                ?.apply()
        } catch (e: Exception) {
            Log.e("AppThemeState", "Error guardando tema", e)
        }
    }
}
