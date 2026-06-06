package com.malla.mvp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.malla.mvp.ui.components.ThemeSelectorCard
import com.malla.mvp.ui.settings.AccessibilitySettings
import com.malla.mvp.ui.theme.MallaColorScheme

@Composable
fun SettingsScreen(
    currentScheme: MallaColorScheme,
    onSchemeSelected: (MallaColorScheme) -> Unit
) {
    val context = LocalContext.current
    var showDiagnostic by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Perfil
        item {
            SettingsCard(title = "Perfil", icon = Icons.Filled.Person) {
                Text("Nombre: Usuario Malla")
                Text("Estado: Conectado")
            }
        }

        // Red Mesh
        item {
            SettingsCard(title = "Red Mesh", icon = Icons.Filled.Wifi) {
                var serverEnabled by remember { mutableStateOf(true) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Servidor TCP", modifier = Modifier.weight(1f))
                    Switch(checked = serverEnabled, onCheckedChange = { serverEnabled = it })
                }
                Text("Intervalo de escaneo: 30s / 5min")
                TextButton(onClick = { showDiagnostic = true }) {
                    Text("Diagnóstico de red")
                }
            }
        }

        // Chats
        item {
            SettingsCard(title = "Chats", icon = Icons.Filled.Chat) {
                val bubbleColors = listOf(
                    null to "Tema",
                    Color(0xFF00E5FF) to "Cyan",
                    Color(0xFF4CAF50) to "Verde",
                    Color(0xFFFF7043) to "Naranja",
                    Color(0xFF9575CD) to "Morado",
                    Color(0xFF78909C) to "Gris"
                )
                Text("Color de burbujas propias", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bubbleColors.forEach { (color, _) ->
                        val isSelected = AccessibilitySettings.ownBubbleColor.value == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color ?: MaterialTheme.colorScheme.primaryContainer)
                                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { AccessibilitySettings.ownBubbleColor.value = color; AccessibilitySettings.save(context) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Color de burbujas del contacto", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bubbleColors.forEach { (color, _) ->
                        val isSelected = AccessibilitySettings.otherBubbleColor.value == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color ?: MaterialTheme.colorScheme.secondaryContainer)
                                .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { AccessibilitySettings.otherBubbleColor.value = color; AccessibilitySettings.save(context) }
                        )
                    }
                }
            }
        }

        // Notificaciones
        item {
            SettingsCard(title = "Notificaciones", icon = Icons.Filled.Notifications) {
                var notifEnabled by remember { mutableStateOf(true) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notificaciones", modifier = Modifier.weight(1f))
                    Switch(checked = notifEnabled, onCheckedChange = { notifEnabled = it })
                }
            }
        }

        // Tema
        item {
            SettingsCard(title = "Tema", icon = Icons.Filled.Palette) {
                ThemeSelectorCard(
                    currentScheme = currentScheme,
                    onSchemeSelected = onSchemeSelected
                )
            }
        }

        // Datos y almacenamiento
        item {
            SettingsCard(title = "Datos y almacenamiento", icon = Icons.Filled.Storage) {
                Text("Uso de almacenamiento")
                TextButton(onClick = { /* Limpiar chats */ }) {
                    Text("Limpiar todas las conversaciones")
                }
            }
        }

        // Seguridad
        item {
            SettingsCard(title = "Seguridad", icon = Icons.Filled.Lock) {
                Text("Cifrado E2E activo")
                Text("Clave pública: ...")
            }
        }

        // Ayuda
        item {
            SettingsCard(title = "Ayuda", icon = Icons.Filled.Info) {
                TextButton(onClick = { /* Acerca de */ }) {
                    Text("Acerca de MALLA")
                }
                TextButton(onClick = { /* Tutorial */ }) {
                    Text("Tutorial de uso")
                }
                TextButton(onClick = { /* Términos */ }) {
                    Text("Términos y privacidad")
                }
            }
        }
    }

    // Diálogo de diagnóstico en vivo
    if (showDiagnostic) {
        AlertDialog(
            onDismissRequest = { showDiagnostic = false },
            title = { Text("Diagnóstico en vivo") },
            text = {
                DiagnosticScreen()
            },
            confirmButton = {
                TextButton(onClick = { showDiagnostic = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            if (expanded) {
                content()
            }
        }
    }
}
