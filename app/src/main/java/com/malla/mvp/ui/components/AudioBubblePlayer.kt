package com.malla.mvp.ui.components

import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AudioBubblePlayer(
    filePath: String,
    modifier: Modifier = Modifier,
    isOwn: Boolean = true,
    avatarBitmap: Bitmap? = null,
    isActiveTrack: Boolean = false,
    onFinished: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    val file = remember(filePath) { File(filePath) }
    val fileExists = file.exists() && file.length() > 0

    if (!fileExists) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Transparent)
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

    val animatedWaveOffset = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animatedWaveOffset.animateTo(
                360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            animatedWaveOffset.stop()
            animatedWaveOffset.snapTo(0f)
        }
    }

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
                onFinished()
            }
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

    LaunchedEffect(isActiveTrack) {
        if (isActiveTrack && !isPlaying && duration > 0) {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying) {
        if (duration <= 0) return@LaunchedEffect
        while (isPlaying) {
            try { currentPosition = mediaPlayer.currentPosition } catch (_: Exception) {}
            delay(50)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap.asImageBitmap(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .align(Alignment.Top),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(40.dp).clip(CircleShape),
                color = Color(0xFF4CE6FF).copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isOwn) "M" else "?",
                        color = Color(0xFF4CE6FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (duration <= 0) return@IconButton
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else {
                    mediaPlayer.start()
                    isPlaying = true
                    onPlay()  // notificar inicio manual
                }
            },
            modifier = Modifier.size(36.dp).scale(if (isPlaying) 1.1f else 1f)
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF4CE6FF),
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val waveHeight = size.height
                val waveWidth = size.width
                val step = 8f
                val amplitude = if (isPlaying) waveHeight * 0.4f else 2f
                val currentWaveOffset = animatedWaveOffset.value
                val path = Path()
                path.moveTo(0f, waveHeight / 2)
                for (x in 0..waveWidth.toInt() step step.toInt()) {
                    val y = waveHeight / 2 + amplitude * kotlin.math.sin((x + currentWaveOffset) * 0.05f).toFloat()
                    path.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF4CE6FF).copy(alpha = if (isPlaying) 0.8f else 0.3f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
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
