package com.malla.mvp.ui.settings

enum class BubbleSize(val label: String, val maxWidthFraction: Float, val paddingDp: Int) {
    COMPACT("Compacto", 0.65f, 6),
    NORMAL("Normal", 0.8f, 12),
    SPACIOUS("Espacioso", 0.95f, 18)
}
