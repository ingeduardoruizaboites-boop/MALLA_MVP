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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LockScreenPremium() {
    val infiniteTransition = rememberInfiniteTransition(label = "lock")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "ring"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1B2A), Color(0xFF0A1118)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo animado
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(Color(0xFF4CE6FF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("M", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF4CE6FF))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("MALLA", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Usa tu huella o PIN para desbloquear", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8899AA))
        }
    }
}
