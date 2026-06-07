package com.malla.mvp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.network.ConnectivityMonitor

@Composable
fun DiagnosticScreen() {
    val logs by LogBuffer.logs.collectAsState()
    val isOnline by ConnectivityMonitor.isOnline.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFD700).copy(alpha = 0.2f))
            ) {
                Text(
                    text = if (isOnline) "🌐 ONLINE (mesh inactivo)" else "🕸️ MESH ACTIVO",
                    modifier = Modifier.padding(12.dp),
                    color = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFFFD700),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        items(logs) { log ->
            Text(
                text = log,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}
