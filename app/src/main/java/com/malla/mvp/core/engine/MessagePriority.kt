package com.malla.mvp.core.engine

enum class MessagePriority(val level: Int) {
    EMERGENCY(0),
    HIGH(1),
    NORMAL(2),
    BACKGROUND(3)
}
