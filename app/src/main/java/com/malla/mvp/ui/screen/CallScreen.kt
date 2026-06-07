package com.malla.mvp.ui.screen

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    contactName: String = "Contacto",
    callType: String = "voice",
    onEndCall: () -> Unit = {}
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var callDuration by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var ringtonePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Iniciar tono de llamada
    LaunchedEffect(Unit) {
        val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val player = MediaPlayer.create(context, ringtoneUri)
        if (player != null) {
            player.isLooping = true
            player.start()
            ringtonePlayer = player
        }
        // Temporizador
        while (true) {
            delay(1000)
            callDuration++
        }
    }

    // Limpiar al salir
    DisposableEffect(Unit) {
        onDispose {
            ringtonePlayer?.stop()
            ringtonePlayer?.release()
            audioManager.isSpeakerphoneOn = false
            audioManager.setMicrophoneMute(false)
        }
    }

    // Control de altavoz
    LaunchedEffect(isSpeakerOn) {
        audioManager.isSpeakerphoneOn = isSpeakerOn
    }

    // Control de mute
    LaunchedEffect(isMuted) {
        try {
            audioManager.setMicrophoneMute(isMuted)
        } catch (e: Exception) {
            // Algunos dispositivos no permiten silenciar el micrófono del sistema
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
                FloatingActionButton(
                    onClick = { isMuted = !isMuted },
                    containerColor = Color.White.copy(alpha = if (isMuted) 0.8f else 0.2f)
                ) {
                    Icon(
                        if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Micrófono",
                        tint = if (isMuted) Color.Red else Color.White
                    )
                }
                FloatingActionButton(onClick = onEndCall, containerColor = Color(0xFFFF1744)) {
                    Icon(Icons.Filled.CallEnd, contentDescription = "Colgar", tint = Color.White)
                }
                FloatingActionButton(
                    onClick = { isSpeakerOn = !isSpeakerOn },
                    containerColor = Color.White.copy(alpha = if (isSpeakerOn) 0.8f else 0.2f)
                ) {
                    Icon(
                        if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = "Altavoz",
                        tint = Color.White
                    )
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
