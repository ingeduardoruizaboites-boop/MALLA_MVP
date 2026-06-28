package com.malla.mvp.ui.components

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

object StickerState {
    private val _showPicker = MutableStateFlow(false)
    val showPicker: StateFlow<Boolean> = _showPicker

    private val _fullScreenSticker = MutableStateFlow<String?>(null)
    val fullScreenSticker: StateFlow<String?> = _fullScreenSticker

    var onSendSticker: ((String) -> Unit)? = null

    fun openPicker() {
        _showPicker.value = true
    }

    fun closePicker() {
        _showPicker.value = false
    }

    fun showFullScreen(url: String) {
        _fullScreenSticker.value = url
        closePicker()
    }

    fun dismissFullScreen() {
        _fullScreenSticker.value = null
    }
}
