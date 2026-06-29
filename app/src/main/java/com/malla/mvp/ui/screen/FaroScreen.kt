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
import com.malla.mvp.di.Injector

@Composable
fun FaroScreen() {
    val context = LocalContext.current
    val flashlight = remember { Injector.flashlightTransport }
    var message by remember { mutableStateOf("") }
    var isTransmitting by remember { mutableStateOf(false) }
    var isReceiving by remember { mutableStateOf(false) }
    var receivedText by remember { mutableStateOf("") }
    val progress by flashlight.getTransmissionProgress().collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("FARO", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
        Text("Comunicación óptica de emergencia", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Mensaje a transmitir") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
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
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.FlashOn, null)
            Spacer(Modifier.width(8.dp))
            Text("Transmitir")
        }

        if (isTransmitting) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary)
            Text("Transmitiendo...", color = MaterialTheme.colorScheme.primary)
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Button(
            onClick = {
                isReceiving = true
                flashlight.startReceiving()
                Toast.makeText(context, "Escuchando...", Toast.LENGTH_SHORT).show()
            },
            enabled = !isTransmitting && !isReceiving,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Detener recepción")
        }

        if (receivedText.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(receivedText, modifier = Modifier.padding(16.dp), color = Color.White, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
