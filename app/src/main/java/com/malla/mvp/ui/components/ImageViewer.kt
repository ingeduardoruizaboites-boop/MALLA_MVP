package com.malla.mvp.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun ImageViewer() {
    val uri by ImageViewerState.uri.collectAsState()
    if (uri == null) return

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showTools by remember { mutableStateOf(false) }
    var lines by remember { mutableStateOf<List<Pair<Offset, Offset>>>(emptyList()) }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }
    var currentLineStart by remember { mutableStateOf<Offset?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))) {
        // Imagen con zoom y desplazamiento
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset = offset + pan
                    }
                }
        )

        // Barra de herramientas flotante
        if (showTools) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111D28).copy(alpha = 0.9f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emojis
                IconButton(onClick = { selectedEmoji = "❤️" }) { Text("❤️", style = MaterialTheme.typography.titleMedium) }
                IconButton(onClick = { selectedEmoji = "🔥" }) { Text("🔥", style = MaterialTheme.typography.titleMedium) }
                IconButton(onClick = { selectedEmoji = "😂" }) { Text("😂", style = MaterialTheme.typography.titleMedium) }
                IconButton(onClick = { selectedEmoji = "😮" }) { Text("😮", style = MaterialTheme.typography.titleMedium) }
                // Dibujo libre
                IconButton(onClick = { currentLineStart = null }) {
                    Icon(Icons.Filled.Draw, null, tint = Color(0xFF4CE6FF))
                }
                // Guardar
                IconButton(onClick = { /* guardar */ }) {
                    Icon(Icons.Filled.Save, null, tint = Color(0xFF4CE6FF))
                }
                // Cerrar
                IconButton(onClick = { ImageViewerState.dismiss() }) {
                    Icon(Icons.Filled.Close, null, tint = Color(0xFF4CE6FF))
                }
            }
        }

        // Botón para mostrar/ocultar herramientas
        FloatingActionButton(
            onClick = { showTools = !showTools },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            containerColor = Color(0xFF111D28).copy(alpha = 0.8f)
        ) {
            Icon(Icons.Filled.Edit, null, tint = Color(0xFF4CE6FF))
        }
    }
}
