package com.malla.mvp.ui.settings

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object AccessibilitySettings {
    var bubbleStyle = mutableStateOf(BubbleStyle.MODERN)
    private const val PREFS_NAME = "accessibility"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_HIGH_CONTRAST = "high_contrast"
    private const val KEY_OWN_BUBBLE_COLOR = "own_bubble_color"
    private const val KEY_OTHER_BUBBLE_COLOR = "other_bubble_color"

    var fontScale = mutableFloatStateOf(1.0f)
    var highContrast = mutableStateOf(false)

    // Colores personalizables para las burbujas (null = usar color del tema)
    var ownBubbleColor = mutableStateOf<Color?>(null)
    var otherBubbleColor = mutableStateOf<Color?>(null)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        fontScale.floatValue = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        highContrast.value = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
        val styleName = prefs.getString("bubble_style", BubbleStyle.MODERN.name) ?: BubbleStyle.MODERN.name
        bubbleStyle.value = BubbleStyle.valueOf(styleName)

        // Cargar colores de burbuja
        val ownColorLong = prefs.getLong(KEY_OWN_BUBBLE_COLOR, -1L)
        ownBubbleColor.value = if (ownColorLong != -1L) Color(ownColorLong.toULong()) else null
        val otherColorLong = prefs.getLong(KEY_OTHER_BUBBLE_COLOR, -1L)
        otherBubbleColor.value = if (otherColorLong != -1L) Color(otherColorLong.toULong()) else null
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putFloat(KEY_FONT_SCALE, fontScale.floatValue)
            putBoolean(KEY_HIGH_CONTRAST, highContrast.value)
            putString("bubble_style", bubbleStyle.value.name)
            putLong(KEY_OWN_BUBBLE_COLOR, ownBubbleColor.value?.value?.toLong() ?: -1L)
            putLong(KEY_OTHER_BUBBLE_COLOR, otherBubbleColor.value?.value?.toLong() ?: -1L)
            apply()
        }
    }
}
