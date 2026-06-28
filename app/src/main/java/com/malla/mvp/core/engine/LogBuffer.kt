package com.malla.mvp.core.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LogBuffer {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    fun add(tag: String, message: String) {
        _logs.value = _logs.value + "[${System.currentTimeMillis() % 100000}] $tag: $message"
    }
}
