package com.malla.mvp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.media.VoiceRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VoiceRecorderPanel(
    voiceRecorder: VoiceRecorder,
    onSend: (File) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dragOffsetX by remember { mutableStateOf(0f) }
    val isRecording = voiceRecorder.isRecording  // estado real del grabador

    // Efecto de cancelación si se arrastra lo suficiente
    LaunchedEffect(dragOffsetX) {
        if (dragOffsetX < -250f) {
            voiceRecorder.stopRecording()
            onCancel()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1B2A).copy(alpha = 0.95f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Osciloscopio
        val amp by voiceRecorder.amplitude.collectAsState()
        val infiniteTransition = rememberInfiniteTransition(label = "osc")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Restart),
            label = "phase"
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            val centerY = size.height / 2
            val baseAmplitude = size.height / 2 * 0.6f
            val voiceAmp = (amp / 32767f).coerceIn(0.2f, 1f)
            drawLine(
                color = Color(0xFF4CE6FF).copy(alpha = 0.2f),
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 1f
            )
            val path = Path()
            path.moveTo(0f, centerY)
            for (x in 0..size.width.toInt() step 2) {
                val t = x * 0.05f + phase * Math.PI.toFloat() / 180f
                val y = centerY + baseAmplitude * (
                    kotlin.math.sin(t).toFloat() * 0.4f * voiceAmp +
                    kotlin.math.sin(2 * t).toFloat() * 0.2f * voiceAmp +
                    kotlin.math.cos(3 * t).toFloat() * 0.1f * voiceAmp
                )
                path.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = path,
                color = Color(0xFF4CE6FF).copy(alpha = 0.9f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Botón arrastrable (solo horizontal)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4C4C).copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffsetX = 0f },
                        onDragEnd = {
                            if (dragOffsetX < -200f) {
                                voiceRecorder.stopRecording()
                                onCancel()
                            } else {
                                // Soltar sin arrastrar suficiente -> enviar
                                val file = voiceRecorder.stopRecording()
                                if (file != null && file.length() > 0) {
                                    onSend(file)
                                } else {
                                    onCancel()
                                }
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetX += dragAmount
                        }
                    )
                }
                .graphicsLayer { translationX = dragOffsetX },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Mic, "Grabando", tint = Color(0xFFFF4C4C), modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Desliza ← para cancelar",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}
