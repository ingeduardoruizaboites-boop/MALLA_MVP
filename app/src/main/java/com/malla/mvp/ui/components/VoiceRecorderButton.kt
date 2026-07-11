package com.malla.mvp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.malla.mvp.media.VoiceRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VoiceRecorderButton(
    voiceRecorder: VoiceRecorder,
    onRecordingStarted: () -> Unit,
    onRecordingStopped: (file: File?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showPanel by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    Box(modifier = modifier) {
        // Botón de micrófono
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isRecording) Color(0xFFFF4C4C).copy(alpha = 0.2f) else Color(0xFF4CE6FF).copy(alpha = 0.1f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (!isRecording) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    val file = voiceRecorder.startRecording()
                                    if (file != null) {
                                        isRecording = true
                                        showPanel = true
                                        onRecordingStarted()
                                    } else {
                                        Toast.makeText(context, "Error al iniciar grabación", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val scale by pulse.animateFloat(1f, 1.15f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "s")
                Icon(
                    Icons.Filled.Mic,
                    "Grabando",
                    tint = Color(0xFFFF4C4C),
                    modifier = Modifier.size(24.dp).scale(scale)
                )
            } else {
                Icon(Icons.Filled.Mic, "Grabar", tint = Color(0xFF4CE6FF), modifier = Modifier.size(24.dp))
            }
        }

        // Panel de arrastre
        AnimatedVisibility(
            visible = showPanel,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .offset { IntOffset(0, -100) }
                    .background(Color(0xFF0A1B2A).copy(alpha = 0.95f))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragOffset = Offset.Zero },
                            onDragEnd = {
                                if (dragOffset.x < -200f) {
                                    // Cancelar
                                    voiceRecorder.stopRecording()
                                    isRecording = false
                                    showPanel = false
                                    onRecordingStopped(null)
                                } else {
                                    // Enviar
                                    val file = voiceRecorder.stopRecording()
                                    isRecording = false
                                    showPanel = false
                                    onRecordingStopped(file)
                                }
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += Offset(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Desliza ← para cancelar   •   Desliza ↑ para enviar",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4C4C).copy(alpha = 0.3f))
                            .graphicsLayer {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Mic, "Grabando", tint = Color(0xFFFF4C4C), modifier = Modifier.size(32.dp))
                    }
                }
                Text(
                    "Cancelar",
                    color = Color(0xFFFF4C4C),
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                )
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Enviar",
                    tint = Color(0xFF4CE6FF),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size(24.dp)
                )
            }
        }
    }
}
