package com.malla.mvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    onDismiss: () -> Unit,
    onImage: () -> Unit,
    onVideo: () -> Unit,
    onDocument: () -> Unit,
    onVoiceNote: () -> Unit,
    onLocation: () -> Unit,
    onContact: () -> Unit,
    onSticker: () -> Unit,
    onCamera: () -> Unit
) {
    var viewOnce by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Enviar multimedia", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Primera fila
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onImage(); onDismiss() }) {
                        Icon(Icons.Filled.Photo, "Imagen", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Imagen", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onVideo(); onDismiss() }) {
                        Icon(Icons.Filled.Videocam, "Vídeo", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Vídeo", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onDocument(); onDismiss() }) {
                        Icon(Icons.Filled.InsertDriveFile, "Documento", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Documento", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onVoiceNote(); onDismiss() }) {
                        Icon(Icons.Filled.KeyboardVoice, "Nota de voz", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Nota de voz", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segunda fila
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onLocation(); onDismiss() }) {
                        Icon(Icons.Filled.LocationOn, "Ubicación", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Ubicación", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onContact(); onDismiss() }) {
                        Icon(Icons.Filled.PersonAdd, "Contacto", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Contacto", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onSticker(); onDismiss() }) {
                        Icon(Icons.Filled.InsertEmoticon, "Sticker", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Sticker", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onCamera(); onDismiss() }) {
                        Icon(Icons.Filled.CameraAlt, "Cámara", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Cámara", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver una vez", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Switch(
                    checked = viewOnce,
                    onCheckedChange = { viewOnce = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
