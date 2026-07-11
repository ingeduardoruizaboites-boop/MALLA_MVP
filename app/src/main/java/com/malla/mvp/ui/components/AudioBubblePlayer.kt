package com.malla.mvp.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AudioBubblePlayer(filePath: String, modifier: Modifier = Modifier) {
    val file = remember(filePath) { File(filePath) }
    val fileExists = file.exists() && file.length() > 0

    if (!fileExists) {
        android.widget.Toast.makeText(android.app.Application(), "Audio no disponible: archivo vacío o inexistente", android.widget.Toast.LENGTH_SHORT).show()
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A3A4A))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Error, "Error", tint = Color(0xFFFF4C4C), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("Audio no disponible", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        return
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    val mediaPlayer = remember { MediaPlayer() }
    val infiniteTransition = rememberInfiniteTransition(label = "audio")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "wave"
    )

    LaunchedEffect(filePath) {
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            duration = mediaPlayer.duration
        } catch (_: Exception) {
            duration = 0
        }

        if (duration > 0) {
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                currentPosition = 0
                mediaPlayer.seekTo(0)
            }
        } else {
            isPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Actualizar progreso solo si está reproduciendo y duration válida
    LaunchedEffect(isPlaying) {
        if (duration <= 0) {
            isPlaying = false
            return@LaunchedEffect
        }
        while (isPlaying) {
            try {
                currentPosition = mediaPlayer.currentPosition
            } catch (_: Exception) {}
            delay(50)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A3A4A), Color(0xFF0A1B2A))))
            .padding(8.dp)
    ) {
        IconButton(
            onClick = {
                if (duration <= 0) return@IconButton
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else {
                    mediaPlayer.start()
                    isPlaying = true
                }
            },
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
            LinearProgressIndicator(
                progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4CE6FF),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(2.dp))
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
