package com.malla.mvp.ui.settings

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

object AccessibilitySettings {
    var bubbleStyle = mutableStateOf(BubbleStyle.MODERN)
    private const val PREFS_NAME = "accessibility"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_HIGH_CONTRAST = "high_contrast"

    var fontScale = mutableFloatStateOf(1.0f)
    var highContrast = mutableStateOf(false)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        fontScale.floatValue = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        highContrast.value = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
        val styleName = prefs.getString("bubble_style", BubbleStyle.MODERN.name) ?: BubbleStyle.MODERN.name
        bubbleStyle.value = BubbleStyle.valueOf(styleName)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putFloat(KEY_FONT_SCALE, fontScale.floatValue)
            putBoolean(KEY_HIGH_CONTRAST, highContrast.value)
            putString("bubble_style", bubbleStyle.value.name)
        apply()
        }
    }
}
