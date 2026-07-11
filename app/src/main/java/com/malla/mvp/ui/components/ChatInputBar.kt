package com.malla.mvp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.malla.mvp.media.VoiceRecorder
import java.io.File

@Composable
fun ChatInputBar(
    voiceRecorder: VoiceRecorder,
    onSendText: (String) -> Unit,
    onSendVoice: (File) -> Unit,
    onSendZumbido: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        AnimatedVisibility(
            visible = !isRecording,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            IconButton(onClick = onSendZumbido) {
                Icon(Icons.Filled.Vibration, "Zumbido", tint = Color(0xFF4CE6FF))
            }
        }

        AnimatedContent(
            targetState = isRecording,
            transitionSpec = {
                if (!targetState) {
                    (slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200)))
                } else {
                    (slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)))
                }
            },
            modifier = Modifier.weight(1f).heightIn(max = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) { recording ->
            if (!recording) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Mensaje", color = Color.Gray) },
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 16.sp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CE6FF),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedContainerColor = Color(0xFF1A2A3A),
                        unfocusedContainerColor = Color(0xFF1A2A3A)
                    ),
                    leadingIcon = {
                        IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                            Icon(Icons.Filled.InsertEmoticon, "Emoji", tint = Color(0xFF4CE6FF))
                        }
                    },
                    trailingIcon = {
                        Row(
                            modifier = Modifier.animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                        ) {
                            IconButton(onClick = { /* TODO attachment */ }) {
                                Icon(Icons.Filled.AttachFile, "Adjuntar", tint = Color(0xFF4CE6FF))
                            }
                            if (text.isBlank()) {
                                IconButton(onClick = { /* TODO cámara */ }) {
                                    Icon(Icons.Filled.CameraAlt, "Cámara", tint = Color(0xFF4CE6FF))
                                }
                            }
                        }
                    }
                )
            } else {
                EqualizerBarsIndicator(
                    amplitudeFlow = voiceRecorder.amplitude,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
            }
        }

        if (text.isBlank() || isRecording) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color(0xFF4CE6FF).copy(alpha = 0.1f) else Color(0xFF4CE6FF).copy(alpha = 0.1f))
                    .pointerInput(isRecording) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                if (change.pressed && !change.previousPressed && !isRecording) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        val file = voiceRecorder.startRecording()
                                        if (file != null) {
                                            isRecording = true
                                        } else {
                                            Toast.makeText(context, "Error al iniciar grabación", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                if (!change.pressed && change.previousPressed && isRecording) {
                                    val file = try { voiceRecorder.stopRecording() } catch (e: Exception) { null }
                                    isRecording = false
                                    if (file != null && file.length() > 0) {
                                        onSendVoice(file)
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    val pulse = rememberInfiniteTransition(label = "micPulse")
                    val micScale by pulse.animateFloat(1f, 1.15f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "s")
                    Icon(Icons.Filled.Mic, "Grabando", tint = Color(0xFFFF4C4C), modifier = Modifier.size(24.dp).scale(micScale))
                } else {
                    Icon(Icons.Filled.Mic, "Grabar", tint = Color(0xFF4CE6FF), modifier = Modifier.size(24.dp))
                }
            }
        } else {
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    onSendText(text)
                    text = ""
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color(0xFF4CE6FF))
            }
        }
    }

    if (showEmojiPicker) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A38))
        ) {
            val emojis = listOf("😀","😂","😍","😢","😡","👍","👋","🎉","❤️","🔥","😎","🙏","💪","🤔","😴","🥳")
            Column(modifier = Modifier.padding(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    emojis.take(8).forEach { emoji ->
                        Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(4.dp).clickable {
                            text = text + emoji; showEmojiPicker = false
                        })
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    emojis.drop(8).forEach { emoji ->
                        Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(4.dp).clickable {
                            text = text + emoji; showEmojiPicker = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerBarsIndicator(
    amplitudeFlow: kotlinx.coroutines.flow.StateFlow<Int>,
    modifier: Modifier = Modifier,
    barCount: Int = 20,
    primaryColor: Color = Color(0xFF4CE6FF)
) {
    val amp by amplitudeFlow.collectAsState()

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = canvasWidth / (barCount * 2f) // gap between bars
        val maxBarHeight = canvasHeight * 0.8f
        val normalizedAmp = (amp / 32767f).coerceIn(0.05f, 1f)

        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount
            // simulate different heights with variation based on amplitude and a wave pattern
            val barHeight = maxBarHeight * normalizedAmp * (0.4f + 0.6f * kotlin.math.sin(fraction * Math.PI.toFloat()).toFloat())
            val x = i * (barWidth * 2f) // each bar occupies barWidth and gap
            val y = canvasHeight - barHeight
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
            )
        }
    }
}
