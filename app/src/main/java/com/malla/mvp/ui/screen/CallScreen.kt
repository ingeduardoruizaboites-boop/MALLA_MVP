package com.malla.mvp.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CallScreen(
    contactName: String = "Contacto",
    callType: String = "voice",
    onEndCall: () -> Unit = {}
) {
    var callDuration by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            callDuration++
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A1B2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(contactName, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (callType == "video") "Videollamada" else "Llamada de voz",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8899AA)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = formatDuration(callDuration),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(48.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                FloatingActionButton(onClick = { isMuted = !isMuted }, containerColor = Color.White.copy(alpha = 0.2f)) {
                    Icon(if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, "Mic", tint = Color.White)
                }
                FloatingActionButton(onClick = onEndCall, containerColor = Color(0xFFFF1744)) {
                    Icon(Icons.Filled.CallEnd, "Colgar", tint = Color.White)
                }
                FloatingActionButton(onClick = { isSpeakerOn = !isSpeakerOn }, containerColor = Color.White.copy(alpha = 0.2f)) {
                    Icon(if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, "Altavoz", tint = Color.White)
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}
