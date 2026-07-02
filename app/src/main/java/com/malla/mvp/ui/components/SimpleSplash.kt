package com.malla.mvp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SimpleSplash() {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        LaunchedEffect(Unit) {
            delay(1500)
            showSplash = false
        }
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0A1118)),
            contentAlignment = Alignment.Center
        ) {
            Text("M A L L A", color = Color(0xFF4CE6FF), fontSize = 32.sp)
        }
    } else {
        Text("App cargada")
    }
}
