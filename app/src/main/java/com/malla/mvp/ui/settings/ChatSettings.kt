package com.malla.mvp.ui.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ChatSettings {
    private const val PREF_NAME = "chat_prefs"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_BUBBLE_OPACITY = "bubble_opacity"
    private const val KEY_OWN_TEXT_COLOR = "own_text_color"
    private const val KEY_OTHER_TEXT_COLOR = "other_text_color"

    private val _fontSize = MutableStateFlow(14f)
    val fontSize: StateFlow<Float> = _fontSize

    private val _bubbleOpacity = MutableStateFlow(1f)
    val bubbleOpacity: StateFlow<Float> = _bubbleOpacity

    private val _ownTextColor = MutableStateFlow<Color?>(null)
    val ownTextColor: StateFlow<Color?> = _ownTextColor

    private val _otherTextColor = MutableStateFlow<Color?>(null)
    val otherTextColor: StateFlow<Color?> = _otherTextColor

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _fontSize.value = prefs.getFloat(KEY_FONT_SIZE, 14f)
        _bubbleOpacity.value = prefs.getFloat(KEY_BUBBLE_OPACITY, 1f)

        val ownColorInt = prefs.getInt(KEY_OWN_TEXT_COLOR, -1)
        _ownTextColor.value = if (ownColorInt != -1) Color(ownColorInt) else null

        val otherColorInt = prefs.getInt(KEY_OTHER_TEXT_COLOR, -1)
        _otherTextColor.value = if (otherColorInt != -1) Color(otherColorInt) else null
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_FONT_SIZE, _fontSize.value)
            .putFloat(KEY_BUBBLE_OPACITY, _bubbleOpacity.value)
            .putInt(KEY_OWN_TEXT_COLOR, _ownTextColor.value?.toArgb() ?: -1)
            .putInt(KEY_OTHER_TEXT_COLOR, _otherTextColor.value?.toArgb() ?: -1)
            .apply()
    }

    fun updateFontSize(newSize: Float, context: Context) {
        _fontSize.value = newSize
        save(context)
    }

    fun updateBubbleOpacity(newOpacity: Float, context: Context) {
        _bubbleOpacity.value = newOpacity
        save(context)
    }

    fun updateOwnTextColor(color: Color?, context: Context) {
        _ownTextColor.value = color
        save(context)
    }

    fun updateOtherTextColor(color: Color?, context: Context) {
        _otherTextColor.value = color
        save(context)
    }
}
