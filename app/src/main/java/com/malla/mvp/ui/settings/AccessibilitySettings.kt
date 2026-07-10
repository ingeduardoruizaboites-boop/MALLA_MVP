package com.malla.mvp.ui.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AccessibilitySettings {
    val bubbleStyle = MutableStateFlow(BubbleStyle.MODERN)
    private const val PREFS_NAME = "accessibility"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_HIGH_CONTRAST = "high_contrast"
    private const val KEY_OWN_BUBBLE_COLOR = "own_bubble_color"
    private const val KEY_OTHER_BUBBLE_COLOR = "other_bubble_color"

    val fontScale = MutableStateFlow(1.0f)
    val highContrast = MutableStateFlow(false)

    val ownBubbleColor = MutableStateFlow<Color?>(null)
    val otherBubbleColor = MutableStateFlow<Color?>(null)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        fontScale.value = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        highContrast.value = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
        val styleName = prefs.getString("bubble_style", BubbleStyle.MODERN.name) ?: BubbleStyle.MODERN.name
        bubbleStyle.value = BubbleStyle.valueOf(styleName)

        val ownColorLong = prefs.getLong(KEY_OWN_BUBBLE_COLOR, -1L)
        ownBubbleColor.value = if (ownColorLong != -1L) Color(ownColorLong.toULong()) else null
        val otherColorLong = prefs.getLong(KEY_OTHER_BUBBLE_COLOR, -1L)
        otherBubbleColor.value = if (otherColorLong != -1L) Color(otherColorLong.toULong()) else null
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putFloat(KEY_FONT_SCALE, fontScale.value)
            putBoolean(KEY_HIGH_CONTRAST, highContrast.value)
            putString("bubble_style", bubbleStyle.value.name)
            putLong(KEY_OWN_BUBBLE_COLOR, ownBubbleColor.value?.value?.toLong() ?: -1L)
            putLong(KEY_OTHER_BUBBLE_COLOR, otherBubbleColor.value?.value?.toLong() ?: -1L)
            apply()
        }
    }
}
