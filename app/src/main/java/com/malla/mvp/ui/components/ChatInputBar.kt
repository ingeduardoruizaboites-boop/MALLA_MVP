package com.malla.mvp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    isMeshMode: Boolean,
    onVibrate: () -> Unit,
    onAttachmentClick: () -> Unit,
    onSend: () -> Unit,
    placeholder: String = "Mensaje",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMeshMode) {
                IconButton(onClick = onVibrate) {
                    Icon(Icons.Filled.Vibration, "Zumbido", tint = MaterialTheme.colorScheme.primary)
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder) },
                maxLines = 1,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            if (!isMeshMode) {
                IconButton(onClick = onAttachmentClick) {
                    Icon(Icons.Filled.AttachFile, "Adjuntar", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (text.isBlank()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { if (!isRecording) onStartRecording() },
                                onTap = { if (isRecording) onStopRecording() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Stop, "Detener", tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(4) { i ->
                                    val infiniteTransition = rememberInfiniteTransition(label = "bar_$i")
                                    val height by infiniteTransition.animateFloat(
                                        initialValue = 8f,
                                        targetValue = 24f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(300, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bar_height_$i"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(height.dp)
                                            .padding(horizontal = 2.dp)
                                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    } else {
                        Icon(Icons.Filled.Mic, "Grabar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                IconButton(onClick = onSend) {
                    Icon(Icons.Filled.Send, "Enviar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
