package com.malla.mvp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A1B2A), Color(0xFF0A1118)))
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gestos de MALLA",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF4CE6FF),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = steps[step].third,
                contentDescription = steps[step].first,
                tint = Color(0xFF4CE6FF),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = steps[step].first,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = steps[step].second,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (index == step) Color(0xFF4CE6FF) else Color.White.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { if (step > 0) step-- }) {
                    Text("Anterior", color = Color(0xFF8899AA))
                }
                Button(
                    onClick = {
                        if (step < steps.size - 1) step++
                        else onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CE6FF),
                        contentColor = Color(0xFF0A1B2A)
                    )
                ) {
                    Text(
                        if (step < steps.size - 1) "Siguiente" else "Entendido",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
