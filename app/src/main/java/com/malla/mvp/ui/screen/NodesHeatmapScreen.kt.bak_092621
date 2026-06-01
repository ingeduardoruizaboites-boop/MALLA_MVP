package com.malla.mvp.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.malla.mvp.network.NetworkService
import com.malla.mvp.util.QrCodeGenerator

@Composable
fun NodesHeatmapScreen(
    onNavigateToQrScanner: () -> Unit,
    onConnectToPeer: (String) -> Unit
) {
    val localIp = remember { NetworkService.getLocalIpAddress() }
    val connectedCount = 0
    var showMyQr by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var manualIp by remember { mutableStateOf("") }

    LaunchedEffect(showMyQr) {
        if (showMyQr) {
            qrBitmap = QrCodeGenerator.generateQrCode(localIp, 400, 400)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tarjeta IP
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tu IP", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(localIp, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conexión manual
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Conectar a un peer", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text("Dirección IP") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (manualIp.isNotBlank()) {
                            onConnectToPeer(manualIp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conectar")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QR
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showMyQr = true }) { Text("Mi QR") }
            OutlinedButton(onClick = onNavigateToQrScanner) { Text("Escanear QR") }
        }
    }

    if (showMyQr && qrBitmap != null) {
        AlertDialog(
            onDismissRequest = { showMyQr = false },
            title = { Text("Mi código QR") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(250.dp))
                    Text("IP: $localIp")
                }
            },
            confirmButton = { TextButton(onClick = { showMyQr = false }) { Text("Cerrar") } }
        )
    }
}
