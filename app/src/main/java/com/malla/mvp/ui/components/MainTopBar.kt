package com.malla.mvp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.identity.IdentityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onDiagnosticClick: () -> Unit = {},
    isOnline: Boolean
) {
    val currentAvatar by IdentityManager.avatarBitmap.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = "M A L L A",
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    if (currentAvatar != null) {
                        Image(
                            bitmap = currentAvatar!!.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "M",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.width(220.dp)
                ) {
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Settings, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Ajustes") } },
                        onClick = { showMenu = false; onSettingsClick() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Person, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Perfil") } },
                        onClick = { showMenu = false; onProfileClick() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.BugReport, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Diagnóstico") } },
                        onClick = { showMenu = false; onDiagnosticClick() }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Info, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Acerca de") } },
                        onClick = { showMenu = false }
                    )
                }
            }
        },
        actions = {
            Icon(
                imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.SignalWifiStatusbarConnectedNoInternet4,
                contentDescription = if (isOnline) "Conectado" else "Modo mesh",
                tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 16.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.background(
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    MaterialTheme.colorScheme.surface
                )
            )
        )
    )
}
