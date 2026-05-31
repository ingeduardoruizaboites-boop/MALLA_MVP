package com.malla.mvp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    val steps = listOf(
        Triple("Desliza a la izquierda", "Para archivar, eliminar o ocultar un chat", Icons.Filled.SwipeLeft),
        Triple("Desliza a la derecha", "Para ver el perfil o las historias del contacto", Icons.Filled.SwipeRight),
        Triple("Doble toque", "En un mensaje para reaccionar con ❤️", Icons.Filled.Favorite),
        Triple("Zumbido", "Toca el icono de vibración para enviar un zumbido", Icons.Filled.Vibration)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { /* no cierra hasta el final */ }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gestos de MALLA",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Icono del gesto actual
            Icon(
                imageVector = steps[step].third,
                contentDescription = steps[step].first,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = steps[step].first,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = steps[step].second,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Indicador de pasos
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (index == step) MaterialTheme.colorScheme.primary else Color.Gray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { if (step > 0) step-- }) {
                    Text("Anterior", color = Color.White)
                }
                Button(onClick = {
                    if (step < steps.size - 1) step++
                    else onDismiss()
                }) {
                    Text(if (step < steps.size - 1) "Siguiente" else "Entendido")
                }
            }
        }
    }
}
