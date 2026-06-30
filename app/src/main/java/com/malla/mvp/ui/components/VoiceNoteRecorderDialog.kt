package com.malla.mvp.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.malla.mvp.media.VoiceRecorder
import java.io.File

@Composable
fun VoiceNoteRecorderDialog(
    onDismiss: () -> Unit,
    onVoiceNoteReady: (Uri) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    val recorder = remember { VoiceRecorder(context) }

    AlertDialog(
        onDismissRequest = {
            if (isRecording) recorder.stopRecording()
            onDismiss()
        },
        title = { Text("Nota de voz") },
        text = {
            Column {
                if (isRecording) {
                    Text("Grabando...")
                } else if (recordedFile != null) {
                    Text("Grabación lista")
                } else {
                    Text("Presiona el botón para grabar")
                }
            }
        },
        confirmButton = {
            if (!isRecording && recordedFile == null) {
                TextButton(onClick = {
                    recordedFile = recorder.startRecording()
                    isRecording = true
                }) {
                    Text("Iniciar grabación")
                }
            } else if (isRecording) {
                TextButton(onClick = {
                    recorder.stopRecording()
                    isRecording = false
                }) {
                    Text("Detener")
                }
            } else {
                TextButton(onClick = {
                    recordedFile?.let { file ->
                        val uri = Uri.fromFile(file)
                        onVoiceNoteReady(uri)
                    }
                    onDismiss()
                }) {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isRecording) recorder.stopRecording()
                onDismiss()
            }) {
                Text("Cancelar")
            }
        }
    )
}
