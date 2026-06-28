package com.malla.mvp.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.core.transport.FlashlightTransport

@Composable
fun FaroScreen(flashlight: FlashlightTransport) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var isTransmitting by remember { mutableStateOf(false) }
    var isReceiving by remember { mutableStateOf(false) }
    var receivedText by remember { mutableStateOf("") }
    val progress by flashlight.getTransmissionProgress().collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A1118)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("FARO", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF4CE6FF), fontFamily = FontFamily.Monospace)
        Text("Comunicación óptica de emergencia", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8899AA))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Mensaje a transmitir") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CE6FF))
        )

        Button(
            onClick = {
                if (message.isBlank()) return@Button
                isTransmitting = true
                flashlight.startTransmitting(message)
                Toast.makeText(context, "Transmitiendo...", Toast.LENGTH_SHORT).show()
            },
            enabled = !isTransmitting && !isReceiving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF))
        ) {
            Icon(Icons.Filled.FlashOn, null)
            Spacer(Modifier.width(8.dp))
            Text("Transmitir")
        }

        if (isTransmitting) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF4CE6FF))
            Text("Transmitiendo...", color = Color(0xFF4CE6FF))
        }

        Divider(color = Color.White.copy(alpha = 0.2f))

        Button(
            onClick = {
                isReceiving = true
                flashlight.startReceiving()
                Toast.makeText(context, "Escuchando...", Toast.LENGTH_SHORT).show()
            },
            enabled = !isTransmitting && !isReceiving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF))
        ) {
            Icon(Icons.Filled.Camera, null)
            Spacer(Modifier.width(8.dp))
            Text("Recibir")
        }

        Button(
            onClick = {
                isReceiving = false
                flashlight.stopReceiving()
            },
            enabled = isReceiving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
        ) {
            Text("Detener recepción")
        }

        if (receivedText.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF111D28))) {
                Text(receivedText, modifier = Modifier.padding(16.dp), color = Color.White, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
