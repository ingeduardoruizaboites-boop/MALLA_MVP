package com.malla.mvp.ui.components

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ImageViewerState {
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri: StateFlow<Uri?> = _uri

    fun show(uri: Uri) {
        _uri.value = uri
    }

    fun dismiss() {
        _uri.value = null
    }
}
