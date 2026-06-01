package com.malla.mvp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {
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
                // Contenido del perfil (placeholder)
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
                // Más opciones mesh...
                Text("Intervalo de escaneo: 30s / 5min")
                TextButton(onClick = { /* Navegar a diagnóstico */ }) {
                    Text("Diagnóstico de red")
                }
            }
        }

        // Chats
        item {
            SettingsCard(title = "Chats", icon = Icons.Filled.Chat) {
                Text("Fondo de pantalla")
                Text("Tamaño de fuente")
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
                val availableThemes = listOf(
                    "Cian neón" to Color(0xFF00FFFF),
                    "Magenta neón" to Color(0xFFFF00FF),
                    "Verde neón" to Color(0xFF39FF14),
                    "Naranja neón" to Color(0xFFFF6600),
                    "Morado neón" to Color(0xFFBF00FF),
                    "Blanco" to Color(0xFFFFFFFF)
                )
                availableThemes.forEach { (name, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentColor == color, onClick = { onColorSelected(color) })
                        Text(name)
                    }
                }
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
