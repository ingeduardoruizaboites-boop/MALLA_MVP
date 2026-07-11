package com.malla.mvp.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.identity.IdentityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    onSettingsClick: () -> Unit,
    onChatSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    isOnline: Boolean
) {
    val currentAvatar by IdentityManager.avatarBitmap.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Detectar tipo de conexión
    val connectionIcon = remember(isOnline) {
        if (!isOnline) {
            Icons.Filled.SignalWifiStatusbarConnectedNoInternet4 // Mesh
        } else {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val nc = cm?.getNetworkCapabilities(cm.activeNetwork)
            when {
                nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> Icons.Filled.Wifi
                nc?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> Icons.Filled.SignalCellularAlt
                else -> Icons.Filled.Wifi // fallback
            }
        }
    }

    // Animación de titilación del punto
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    TopAppBar(
        title = {
            Text(
                text = "MALLA",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = Color.White
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF4CE6FF), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            color = Color(0xFF4CE6FF).copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "M",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF4CE6FF)
                                )
                            }
                        }
                    }
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.width(240.dp)
                ) {
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Person, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Perfil") } },
                        onClick = { showMenu = false; onProfileClick() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Settings, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Ajustes") } },
                        onClick = { showMenu = false; onSettingsClick() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.ChatBubble, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Configuración de chat") } },
                        onClick = { showMenu = false; onChatSettingsClick() }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Invitar amigos") } },
                        onClick = { showMenu = false; /* TODO */ }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Help, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Text("Ayuda") } },
                        onClick = { showMenu = false; /* TODO */ }
                    )
                }
            }
        },
        actions = {
            // Indicador de conexión: icono + punto titilante
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = connectionIcon,
                    contentDescription = "Tipo de conexión",
                    tint = if (isOnline) Color(0xFF4CE6FF) else Color(0xFFFF4C4C),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Punto titilante
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(
                        color = if (isOnline) Color.Green.copy(alpha = dotAlpha) else Color.Red.copy(alpha = dotAlpha),
                        radius = size.minDimension / 2
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0A1B2A),
                    Color(0xFF0A1118)
                )
            )
        )
    )
}
