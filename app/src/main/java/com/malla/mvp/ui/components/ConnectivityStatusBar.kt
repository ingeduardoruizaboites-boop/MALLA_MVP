package com.malla.mvp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.core.engine.DeviceState
import com.malla.mvp.core.engine.DeviceStateMonitor
import com.malla.mvp.core.engine.EmergencyMode
import com.malla.mvp.core.engine.MeshLevel

@Composable
fun ConnectivityStatusBar() {
    val deviceState by DeviceStateMonitor.state.collectAsState()
    val level = deviceState.currentLevel
    val bgColor = backgroundColorForLevel(level)
    val isMeshMode = !deviceState.hasInternetConnection
    val isEmergency by EmergencyMode.isActive.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "meshPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(32.dp).clickable { },
        color = if (isEmergency) Color(0xFFB71C1C).copy(alpha = 0.9f) else bgColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconForLevel(level),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp).then(if (isMeshMode) Modifier.scale(pulseScale) else Modifier)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Crossfade(targetState = level, label = "text") { lvl ->
                        Text(
                            text = if (isEmergency) "EMERGENCIA ACTIVA" else statusTextForLevel(lvl, deviceState),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (isMeshMode && !isEmergency) {
                        Text(
                            text = "Escaneando...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                    if (isEmergency) {
                        Text(
                            text = "Todos los radios activos",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMeshMode && !isEmergency) {
                    TextButton(
                        onClick = { EmergencyMode.activate() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("⚠️ EMERGENCIA", fontSize = 9.sp, color = Color(0xFFFFD700))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val batteryColor = when {
                    deviceState.batteryLevel > 50 -> Color(0xFF4CAF50)
                    deviceState.batteryLevel > 20 -> Color(0xFFFFC107)
                    else -> Color(0xFFFF5252)
                }
                Icon(Icons.Filled.BatteryFull, null, tint = batteryColor, modifier = Modifier.size(14.dp))
                Text(text = "${deviceState.batteryLevel}%", color = batteryColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Filled.Circle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(8.dp))
                Text(text = "${deviceState.nearbyNodes}", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

fun backgroundColorForLevel(level: MeshLevel): Color {
    return when (level) {
        MeshLevel.ONLINE_WIFI, MeshLevel.ONLINE_MOBILE -> Color(0xFF1B5E20)
        MeshLevel.WIFI_DIRECT -> Color(0xFF0D47A1)
        MeshLevel.BLE, MeshLevel.BLUETOOTH_CLASSIC -> Color(0xFF001219)
        MeshLevel.ULTRASOUND -> Color(0xFF4A148C)
        MeshLevel.SMS_BRIDGE -> Color(0xFFFF6F00)
        MeshLevel.NO_SIGNAL -> Color(0xFFB71C1C)
        else -> Color(0xFF111111)
    }
}

fun iconForLevel(level: MeshLevel) = when (level) {
    MeshLevel.ONLINE_WIFI -> Icons.Filled.Wifi
    MeshLevel.ONLINE_MOBILE -> Icons.Filled.SignalCellularAlt
    MeshLevel.WIFI_DIRECT -> Icons.Filled.Link
    MeshLevel.BLE -> Icons.Filled.Bluetooth
    MeshLevel.BLUETOOTH_CLASSIC -> Icons.Filled.BluetoothConnected
    MeshLevel.NFC -> Icons.Filled.Nfc
    MeshLevel.ULTRASOUND -> Icons.Filled.GraphicEq
    MeshLevel.SMS_BRIDGE -> Icons.Filled.Sms
    MeshLevel.FLASH_LIGHT -> Icons.Filled.FlashlightOn
    MeshLevel.QR_CODE -> Icons.Filled.QrCode
    MeshLevel.NO_SIGNAL -> Icons.Filled.SignalWifiStatusbarConnectedNoInternet4
    else -> Icons.Filled.SignalWifiStatusbarConnectedNoInternet4
}

fun statusTextForLevel(level: MeshLevel, state: DeviceState): String {
    return when (level) {
        MeshLevel.ONLINE_WIFI -> "Conectado · ${state.nearbyNodes} nodos globales"
        MeshLevel.ONLINE_MOBILE -> "Datos móviles · Modo online"
        MeshLevel.WIFI_DIRECT -> "Red local · ${state.nearbyNodes} nodos cercanos"
        MeshLevel.BLE -> "Bluetooth · ${state.nearbyNodes} nodos"
        MeshLevel.BLUETOOTH_CLASSIC -> "BT Clásico · Modo compatibilidad"
        MeshLevel.NFC -> "NFC · Acerca dispositivos"
        MeshLevel.ULTRASOUND -> "Modo Tierra · Audio activo"
        MeshLevel.SMS_BRIDGE -> "Puente SMS · ${state.pendingMessages} mensajes"
        MeshLevel.FLASH_LIGHT -> "Señal óptica · Línea visual"
        MeshLevel.QR_CODE -> "QR · Escanea para conectar"
        MeshLevel.NO_SIGNAL -> "Sin señal · Modo supervivencia"
        MeshLevel.TCP_DIRECT -> "TCP Directo · Conexión local"
        else -> "Modo desconocido"
    }
}
