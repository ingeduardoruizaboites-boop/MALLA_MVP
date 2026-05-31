package com.malla.mvp.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.malla.mvp.identity.IdentityManager

@Composable
fun PerfilScreen() {
    val context = LocalContext.current

    var userName by remember { mutableStateOf(IdentityManager.getUserName(context)) }
    var userStatus by remember { mutableStateOf(IdentityManager.getUserStatus(context)) }
    var avatarBitmap by remember { mutableStateOf(IdentityManager.loadAvatar(context)) }
    var bannerBitmap by remember { mutableStateOf(IdentityManager.loadBanner(context)) }

    var editName by remember { mutableStateOf(false) }
    var editStatus by remember { mutableStateOf(false) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { avatarBitmap = IdentityManager.saveAvatar(context, it) }
    }

    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { bannerBitmap = IdentityManager.saveBanner(context, it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Banner detrás
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (bannerBitmap != null) {
                Image(
                    bitmap = bannerBitmap!!.asImageBitmap(),
                    contentDescription = "Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {}
            }
            IconButton(
                onClick = { bannerLauncher.launch("image/*") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Edit, "Cambiar banner", tint = Color.White)
            }
        }

        // Avatar superpuesto (centrado en el borde inferior del banner)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 130.dp) // 180 (banner) - 50 (radio del avatar) = 130
        ) {
            Box(modifier = Modifier.size(100.dp)) {
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape), // borde para destacar
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { avatarLauncher.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        "Cambiar avatar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Nombre, estado y edición (debajo del avatar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
                .padding(top = 240.dp) // espacio suficiente para el avatar
        ) {
            // Nombre editable
            if (editName) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    IdentityManager.setUserName(context, userName)
                    editName = false
                }) { Text("Guardar") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(userName, style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { editName = true }) {
                        Icon(Icons.Filled.Edit, "Editar nombre", modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Estado editable
            if (editStatus) {
                OutlinedTextField(
                    value = userStatus,
                    onValueChange = { userStatus = it },
                    label = { Text("Estado") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    IdentityManager.setUserStatus(context, userStatus)
                    editStatus = false
                }) { Text("Guardar") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        userStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    IconButton(onClick = { editStatus = true }) {
                        Icon(Icons.Filled.Edit, "Editar estado", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
