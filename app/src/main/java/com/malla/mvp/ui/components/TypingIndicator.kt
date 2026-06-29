package com.malla.mvp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TypingIndicator(contactName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 0), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 150), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 300), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("$contactName está escribiendo", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8899AA))
        Spacer(modifier = Modifier.width(8.dp))
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(when (i) { 0 -> dot1; 1 -> dot2; else -> dot3 })
                    .clip(CircleShape)
                    .background(Color(0xFF4CE6FF))
            )
            if (i < 2) Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
