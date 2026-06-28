package com.malla.mvp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PulsoScreen(
    onNavigateToQrScanner: () -> Unit = {},
    onConnectToPeer: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Pulso - Red Mesh", color = Color.White, style = MaterialTheme.typography.headlineSmall)
    }
}
