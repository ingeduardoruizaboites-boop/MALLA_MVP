package com.malla.mvp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ContactProfileScreen(contactName: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, "Volver")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.size(100.dp).clip(CircleShape),
                color = Color(0xFF1976D2).copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(contactName.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(contactName, style = MaterialTheme.typography.headlineSmall)
            Text("Estado: Conectado", color = MaterialTheme.colorScheme.primary)
        }
    }
}
