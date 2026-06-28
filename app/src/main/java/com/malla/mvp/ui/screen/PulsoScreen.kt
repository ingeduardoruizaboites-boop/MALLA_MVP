package com.malla.mvp.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.ui.components.SmsFallbackSwitch
import com.malla.mvp.network.NetworkService
import kotlin.math.roundToInt

// Datos simulados de nodos
data class MeshNode(
    val id: String,
    val alias: String?,
    val relativeX: Float,
    val relativeY: Float,
    val hopCount: Int,
    val connectionType: ConnectionType,
    val signalStrength: Int,
    val latencyMs: Int
)

enum class ConnectionType { BLE, WIFI_AWARE, RELAY }

private val sampleNodes = listOf(
    MeshNode("n1", "MR", 0.72f, 0.28f, 1, ConnectionType.BLE, -42, 2),
    MeshNode("n2", "LG", 0.25f, 0.22f, 1, ConnectionType.WIFI_AWARE, -61, 8),
    MeshNode("n3", "JT", 0.78f, 0.68f, 2, ConnectionType.RELAY, -72, 45),
    MeshNode("n4", "AP", 0.2f, 0.72f, 2, ConnectionType.RELAY, -79, 90),
    MeshNode("n5", null, 0.55f, 0.85f, 3, ConnectionType.RELAY, -86, 180)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsoScreen(
    onNavigateToQrScanner: () -> Unit,
    onConnectToPeer: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val localIp = remember { NetworkService.getLocalIpAddress() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Barra superior
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "dot")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "dot"
                    )
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pulso de la Red", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("IP: $localIp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("PULSO") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("NODOS") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("LOGROS") })
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("MODOS") })
                }
            }
        }

        when (selectedTab) {
            0 -> TabPulso()
            1 -> TabNodos()
            2 -> TabLogros()
            3 -> TabModos()
        }
    }
}

@Composable
fun TabPulso() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(value = "5", label = "Nodos", delta = "+2 hoy", deltaPositive = true, modifier = Modifier.weight(1f))
                StatCard(value = "3", label = "Saltos máx.", delta = "~900m", deltaPositive = null, modifier = Modifier.weight(1f))
                StatCard(value = "148", label = "Msgs relay", delta = "+23/h", deltaPositive = true, modifier = Modifier.weight(1f))
            }
        }
        item { MapCard() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "BLE")
                LegendItem(color = Color(0xFF378ADD), label = "Wi-Fi Aware")
                LegendItem(color = Color(0xFF888780), label = "Relay")
            }
        }
        item { UptimeBanner() }
    }
}

@Composable
fun StatCard(value: String, label: String, delta: String, deltaPositive: Boolean?, modifier: Modifier = Modifier) {
    val deltaColor = when {
        deltaPositive == true -> MaterialTheme.colorScheme.primary
        deltaPositive == false -> Color(0xFFE24B4A)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Text(delta, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = deltaColor)
        }
    }
}

@Composable
fun MapCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Restart), label = "mesh"
    )
    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A14)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                // Grid
                for (r in listOf(40f, 70f, 100f, 130f)) {
//                    drawCircle(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), r, center, style = Stroke(width = 0.5f))
                }
                // Pulso
                val pulseRadius = 12f + (animationValue * 20f)
//                drawCircle(MaterialTheme.colorScheme.primary.copy(alpha = (1 - animationValue) * 0.4f), pulseRadius, center, style = Stroke(width = 1.5f))
                // Nodos
                sampleNodes.forEach { node ->
                    val nodeCenter = Offset(size.width * node.relativeX, size.height * node.relativeY)
                    val connColor = when (node.connectionType) {
//                        ConnectionType.BLE -> MaterialTheme.colorScheme.primary
                        ConnectionType.WIFI_AWARE -> Color(0xFF378ADD)
                        ConnectionType.RELAY -> Color(0xFF888780)
                        else -> Color.Gray
                    }
                    if (node.hopCount > 1) {
                        drawLine(connColor.copy(alpha = 0.3f), center, nodeCenter, strokeWidth = 0.8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f))
                    } else {
                        drawLine(connColor.copy(alpha = 0.5f), center, nodeCenter, strokeWidth = 1.2f)
                    }
                    drawCircle(connColor.copy(alpha = 0.85f), if (node.hopCount == 1) 8f else 6f, nodeCenter)
                }
//                drawCircle(MaterialTheme.colorScheme.primary, 10f, center)
            }
            Text("RED MESH · TIEMPO REAL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
            Row(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Circle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("TÚ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun UptimeBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Tiempo activo", style = MaterialTheme.typography.titleSmall)
                Text("6h 34min en red mesh", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun TabNodos() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("NODOS ALCANZABLES AHORA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
        items(sampleNodes) { node -> NodeCard(node) }
    }
}

@Composable
fun NodeCard(node: MeshNode) {
    val typeLabel = when (node.connectionType) {
        ConnectionType.BLE -> "BLE directo"
        ConnectionType.WIFI_AWARE -> "Wi-Fi Aware"
        ConnectionType.RELAY -> "Relay"
    }
    val typeColor = when (node.connectionType) {
        ConnectionType.BLE -> MaterialTheme.colorScheme.primary
        ConnectionType.WIFI_AWARE -> Color(0xFF378ADD)
        ConnectionType.RELAY -> Color(0xFF888780)
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(typeColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(node.alias?.take(2)?.uppercase() ?: "?", style = MaterialTheme.typography.titleSmall, color = typeColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(node.alias ?: "Nodo anónimo", style = MaterialTheme.typography.bodyLarge)
                Text("$typeLabel · ${if (node.hopCount > 1) "${node.hopCount} saltos" else "directo"} · ${node.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            SignalBars(strength = node.signalStrength, color = typeColor)
        }
    }
}

@Composable
fun SignalBars(strength: Int, color: Color) {
    val activeBars = when {
        strength >= -55 -> 5
        strength >= -65 -> 4
        strength >= -75 -> 3
        strength >= -85 -> 2
        else -> 1
    }
    Row(verticalAlignment = Alignment.Bottom) {
        repeat(5) { i ->
            Box(
                modifier = Modifier.width(4.dp).height((5 + i * 4).dp).padding(start = 2.dp)
                    .background(if (i < activeBars) color else Color.White.copy(alpha = 0.12f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun TabLogros() {
    data class Badge(val icon: ImageVector, val title: String, val desc: String, val unlocked: Boolean)
    val badges = listOf(
        Badge(Icons.Filled.WifiTethering, "Primer enlace", "Conectaste tu primer nodo", true),
        Badge(Icons.Filled.Loop, "Relay activo", "100 mensajes", true),
        Badge(Icons.Filled.Language, "Red viva", "6 horas en línea", true),
        Badge(Icons.Filled.Shield, "Nodo guardián", "Bloquea ataques", false),
        Badge(Icons.Filled.Bolt, "Ultra eficiente", "24h solo texto", false),
        Badge(Icons.Filled.Map, "Explorador", "10 nodos alcanzados", false)
    )
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Nivel: Nodo Ancla · 3 de 7", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { 0.43f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary, trackColor = Color.White.copy(alpha = 0.1f))
        Text("148 / 340 msgs relay para nivel 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(24.dp))
        Text("INSIGNIAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))
        // Grid de 3 columnas
        for (row in badges.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { badge ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(0.5.dp, if (badge.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Icon(badge.icon, null, tint = if (badge.unlocked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.38f), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(badge.title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (badge.unlocked) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.38f))
                        Text(badge.desc, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color.White.copy(alpha = 0.38f), textAlign = TextAlign.Center, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                // Rellenar con espacios vacíos si la fila no tiene 3 elementos
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TabModos() {
    var bleActive by remember { mutableStateOf(true) }
    var wifiAwareActive by remember { mutableStateOf(true) }
    var relayActive by remember { mutableStateOf(true) }
    var compressionActive by remember { mutableStateOf(true) }
    var soloTexto by remember { mutableStateOf(false) }

    val batteryImpact = if (soloTexto) 0.04f else {
        (if (bleActive) 0.18f else 0f) + (if (wifiAwareActive) 0.28f else 0f) + (if (relayActive) 0.12f else 0f) + (if (compressionActive) 0.04f else 0f)
    }
    val hoursRemaining = ((1 - batteryImpact) * 36).roundToInt()
    val savingPercent = (batteryImpact * 100).roundToInt()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { Text("RADIOS ACTIVAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
        item { ModeToggleRow(Icons.Filled.Bluetooth, Color(0xFF378ADD), "Bluetooth LE", "Nodos directos hasta 30m", bleActive && !soloTexto, { if (!soloTexto) bleActive = it }) }
        item { ModeToggleRow(Icons.Filled.Wifi, MaterialTheme.colorScheme.primary, "Wi-Fi Direct", "Alcance extendido sin internet", wifiAwareActive && !soloTexto, { if (!soloTexto) wifiAwareActive = it }) }
        item { ModeToggleRow(Icons.Filled.Loop, Color(0xFFEF9F27), "Relay de mensajes", "Reenvío para nodos lejanos", relayActive && !soloTexto, { if (!soloTexto) relayActive = it }) }
        item { ModeToggleRow(Icons.Filled.Compress, Color(0xFF7F77DD), "Compresión", "Reduce peso antes de enviar", compressionActive && !soloTexto, { if (!soloTexto) compressionActive = it }) }
        item { SmsFallbackSwitch() }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = if (soloTexto) Color(0xFF0E1A14) else MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, if (soloTexto) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent)) {
                ModeToggleRow(Icons.Filled.Bolt, MaterialTheme.colorScheme.primary, "⚡ Modo solo texto", "Desactiva todas las radios", soloTexto, { soloTexto = it },
                    titleColor = if (soloTexto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("CONSUMO ESTIMADO DE BATERÍA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Impacto activo: -$savingPercent%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text("~$hoursRemaining h restantes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { (1 - batteryImpact).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (batteryImpact < 0.3f) MaterialTheme.colorScheme.primary else Color(0xFFEF9F27),
                        trackColor = Color.White.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun ModeToggleRow(icon: ImageVector, iconColor: Color, title: String, subtitle: String, value: Boolean, onToggle: (Boolean) -> Unit, titleColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        Switch(checked = value, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}
