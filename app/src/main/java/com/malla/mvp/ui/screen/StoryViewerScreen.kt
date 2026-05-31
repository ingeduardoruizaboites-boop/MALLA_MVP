package com.malla.mvp.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
    imageUri: String,
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    // Temporizador para cerrar automáticamente después de 5 segundos
    LaunchedEffect(Unit) {
        delay(5000)
        visible = false
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Si la uri es un color de placeholder (ej. "#FF5733"), mostramos un fondo de color;
            // en caso contrario, cargamos la imagen desde la web (simulado).
            if (imageUri.startsWith("#")) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color(android.graphics.Color.parseColor(imageUri)))
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUri),
                    contentDescription = "Historia",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }

            // Barra de progreso en la parte superior
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopCenter),
                color = Color.White,
                trackColor = Color.Gray
            )

            // Botón para cerrar
            TextButton(
                onClick = { visible = false; onFinished() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("X", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
