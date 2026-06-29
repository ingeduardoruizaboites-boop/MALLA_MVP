package com.malla.mvp.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AudioBubblePlayer(filePath: String, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "audio")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "wave"
    )

    LaunchedEffect(filePath) {
        mediaPlayer?.release()
        val mp = MediaPlayer().apply {
            try { setDataSource(filePath); prepare(); duration = this.duration } catch (_: Exception) {}
        }
        mediaPlayer = mp
    }

    DisposableEffect(Unit) { onDispose { mediaPlayer?.release() } }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            mediaPlayer?.start()
            while (currentPosition < duration) { delay(50); currentPosition = mediaPlayer?.currentPosition ?: 0 }
            isPlaying = false; currentPosition = 0; mediaPlayer?.seekTo(0)
        } else { mediaPlayer?.pause() }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A3A4A), Color(0xFF0A1B2A))))
            .padding(8.dp)
    ) {
        // Botón play/pause con animación de escala
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier.size(40.dp).scale(if (isPlaying) 1.1f else 1f)
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF4CE6FF),
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            // Ondas de audio animadas
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(20.dp)) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF4CE6FF).copy(alpha = 0.3f + (isPlaying).compareTo(false) * 0.3f))
                            .scale(if (isPlaying) waveScale else 1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Barra de progreso
            LinearProgressIndicator(
                progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4CE6FF),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Tiempos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
