package com.malla.mvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ForwardDialog(
    show: Boolean,
    conversations: List<String>,
    onDismiss: () -> Unit,
    onForward: (String) -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reenviar a...") },
        text = {
            Column {
                conversations.forEach { conv ->
                    TextButton(onClick = { onForward(conv) }, modifier = Modifier.fillMaxWidth()) {
                        Text(conv)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
