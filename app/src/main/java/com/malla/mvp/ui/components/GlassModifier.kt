package com.malla.mvp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.glassCard(): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(
        brush = Brush.verticalGradient(colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f))),
        shape = RoundedCornerShape(16.dp)
    )
    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
    .padding(12.dp)
