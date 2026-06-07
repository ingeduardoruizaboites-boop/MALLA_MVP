package com.malla.mvp.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.ui.components.ThemeSelectorCard
import com.malla.mvp.ui.settings.AccessibilitySettings
import com.malla.mvp.ui.settings.BubbleStyle
import com.malla.mvp.ui.theme.MallaColorScheme
import com.malla.mvp.identity.IdentityManager

@Composable
fun SettingsScreen(
    onShowTutorial: () -> Unit = {},
    currentScheme: MallaColorScheme,
    onSchemeSelected: (MallaColorScheme) -> Unit
) {
    val context = LocalContext.current
    var showDiagnostic by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado
        item {
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Sección Perfil
        item {
            SettingsSection(title = "Perfil") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("👤", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = IdentityManager.getUserName(context),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = IdentityManager.getUserStatus(context),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { /* TODO: Editar perfil */ }) {
                        Icon(Icons.Filled.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Sección Red Mesh
        item {
            SettingsSection(title = "Red Mesh") {
                SettingsItem(title = "Diagnóstico de red", icon = Icons.Filled.BugReport) {
                    showDiagnostic = true
                }
                SettingsItem(title = "Servidor TCP", subtitle = "Intervalo: 30s / 5min", icon = Icons.Filled.Dns) {
                    // TODO: Toggle servidor
                }
            }
        }

        // Sección Chats
        item {
            SettingsSection(title = "Chats") {
                val bubbleColors = listOf(
                    null to "Tema",
                    Color(0xFF00E5FF) to "Cyan",
                    Color(0xFF4CAF50) to "Verde",
                    Color(0xFFFF7043) to "Naranja",
                    Color(0xFF9575CD) to "Morado",
                    Color(0xFF78909C) to "Gris"
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Color de burbujas propias", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bubbleColors.forEach { (color, _) ->
                            val isSelected = AccessibilitySettings.ownBubbleColor.value == color
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color ?: MaterialTheme.colorScheme.primaryContainer)
                                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { AccessibilitySettings.ownBubbleColor.value = color; AccessibilitySettings.save(context) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Color de burbujas del contacto", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bubbleColors.forEach { (color, _) ->
                            val isSelected = AccessibilitySettings.otherBubbleColor.value == color
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color ?: MaterialTheme.colorScheme.secondaryContainer)
                                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { AccessibilitySettings.otherBubbleColor.value = color; AccessibilitySettings.save(context) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Estilo de burbujas", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        BubbleStyle.values().forEach { style ->
                            val isSelected = AccessibilitySettings.bubbleStyle.value == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { AccessibilitySettings.bubbleStyle.value = style; AccessibilitySettings.save(context) },
                                label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sección Notificaciones
        item {
            SettingsSection(title = "Notificaciones") {
                SettingsItem(title = "Activar notificaciones", icon = Icons.Filled.Notifications) {
                    // TODO: Toggle
                }
            }
        }

        // Sección Tema
        item {
            SettingsSection(title = "Tema") {
                ThemeSelectorCard(
                    currentScheme = currentScheme,
                    onSchemeSelected = onSchemeSelected
                )
            }
        }

        // Sección Datos y privacidad
        item {
            SettingsSection(title = "Datos y privacidad") {
                SettingsItem(title = "Cifrado E2E activo", subtitle = "Todas las comunicaciones están protegidas", icon = Icons.Filled.Lock) {}
                SettingsItem(title = "Limpiar todas las conversaciones", icon = Icons.Filled.DeleteSweep) {}
            }
        }

        // Sección Ayuda
        item {
            SettingsSection(title = "Ayuda") {
                SettingsItem(title = "Acerca de MALLA", icon = Icons.Filled.Info) { showAbout = true }
                SettingsItem(title = "Tutorial de uso", icon = Icons.Filled.School) { onShowTutorial() }
                SettingsItem(title = "Términos y privacidad", icon = Icons.Filled.Description) {}
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Acerca de MALLA") },
            text = {
                Column {
                    Text("Versión: 0.1.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("MALLA es una app de mensajería descentralizada que funciona sin internet usando Bluetooth y Wi-Fi Direct.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cifrado E2E · Sin servidores · Código abierto")
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Cerrar") } }
        )
    }
    if (showDiagnostic) {
        AlertDialog(
            onDismissRequest = { showDiagnostic = false },
            title = { Text("Diagnóstico en vivo") },
            text = { DiagnosticScreen() },
            confirmButton = {
                TextButton(onClick = { showDiagnostic = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
    }
}
