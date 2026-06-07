package com.malla.mvp.ui.screen

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.malla.mvp.core.crypto.IdentityQrPayload
import com.malla.mvp.core.crypto.InviteCodeGenerator
import com.malla.mvp.core.crypto.KeystoreManager
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.ui.components.QrCodeDisplay
import kotlinx.coroutines.launch

@Composable
fun PerfilScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Avatar reactivo (interconectado con toda la app)
    val avatarBitmap by IdentityManager.avatarBitmap.collectAsState()

    var userName by remember { mutableStateOf(IdentityManager.getUserName(context)) }
    var userStatus by remember { mutableStateOf(IdentityManager.getUserStatus(context)) }
    var bannerBitmap by remember { mutableStateOf(IdentityManager.loadBanner(context)) }

    var editName by remember { mutableStateOf(false) }
    var editStatus by remember { mutableStateOf(false) }

    // Visibilidad (guardado en SharedPreferences simple)
    val prefs = remember { context.getSharedPreferences("perfil_prefs", Context.MODE_PRIVATE) }
    var nameVisible by remember { mutableStateOf(prefs.getBoolean("name_visible", true)) }
    var statusVisible by remember { mutableStateOf(prefs.getBoolean("status_visible", true)) }

    // QR efímero y código
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var inviteCode by remember { mutableStateOf<InviteCodeGenerator.InviteCode?>(null) }
    var qrExpired by remember { mutableStateOf(false) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { IdentityManager.saveAvatar(context, it) }
    }

    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { bannerBitmap = IdentityManager.saveBanner(context, it) }
    }

    // PubKey truncada
    val pubKeyTruncated = remember { IdentityManager.getIdentityId() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A1B2A), Color(0xFF0A1118)))
            )
    ) {
        // Banner detrás
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp)
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
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            ) {
                Icon(Icons.Filled.Edit, "Cambiar banner", tint = Color.White)
            }
        }

        // Avatar superpuesto
        Box(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier.offset(y = (-50).dp).size(100.dp)
            ) {
                Surface(
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
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
                    modifier = Modifier.align(Alignment.BottomEnd).size(28.dp).offset(x = (-4).dp, y = (-4).dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, "Cambiar avatar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // ── SECCIÓN IDENTIDAD ──────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2233))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tu identidad MALLA", style = MaterialTheme.typography.titleSmall, color = Color(0xFF4CE6FF))
                Spacer(modifier = Modifier.height(8.dp))
                Text(pubKeyTruncated ?: "No disponible", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8899AA))

                Spacer(modifier = Modifier.height(16.dp))

                // QR efímero
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.QrCode, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QR efímero (60s)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                if (qrPayload != null && !qrExpired) {
                    QrCodeDisplay(content = qrPayload!!, size = 160)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Toca para compartir", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8899AA))
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val keystoreManager = KeystoreManager(context)
                                    qrPayload = IdentityQrPayload.generate(keystoreManager, IdentityManager)
                                    qrExpired = false
                                    // Auto-expirar en 60s
                                    kotlinx.coroutines.delay(60_000L)
                                    qrExpired = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                    ) {
                        Text("Generar QR")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Código de 8 caracteres
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Tag, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Código de invitación (24h)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                if (inviteCode != null && !InviteCodeGenerator.isValid(inviteCode)) {
                    Text("Expirado", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4C4C))
                } else if (inviteCode != null) {
                    Text(
                        inviteCode!!.code.chunked(4).joinToString("-"),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                        color = Color(0xFF4CE6FF)
                    )
                } else {
                    Button(
                        onClick = { inviteCode = InviteCodeGenerator.generate() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF).copy(alpha = 0.3f), contentColor = Color(0xFF4CE6FF))
                    ) {
                        Text("Generar código")
                    }
                }
            }
        }

        // ── SECCIÓN PERFIL PÚBLICO ─────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2233))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Perfil público", style = MaterialTheme.typography.titleSmall, color = Color(0xFF4CE6FF))
                Spacer(modifier = Modifier.height(12.dp))

                // Nombre
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (editName) {
                            OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                label = { Text("Nombre") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CE6FF))
                            )
                            Row {
                                Button(onClick = {
                                    IdentityManager.setUserName(context, userName)
                                    editName = false
                                }) { Text("Guardar") }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { editName = false }) { Text("Cancelar") }
                            }
                        } else {
                            Text(userName, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Visible para otros: ${if (nameVisible) "Sí" else "No"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8899AA))
                        }
                    }
                    Switch(
                        checked = nameVisible,
                        onCheckedChange = {
                            nameVisible = it
                            prefs.edit().putBoolean("name_visible", it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CE6FF))
                    )
                    if (!editName) {
                        IconButton(onClick = { editName = true }) {
                            Icon(Icons.Filled.Edit, "Editar nombre", modifier = Modifier.size(20.dp), tint = Color(0xFF4CE6FF))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (editStatus) {
                            OutlinedTextField(
                                value = userStatus,
                                onValueChange = { userStatus = it },
                                label = { Text("Estado") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CE6FF))
                            )
                            Row {
                                Button(onClick = {
                                    IdentityManager.setUserStatus(context, userStatus)
                                    editStatus = false
                                }) { Text("Guardar") }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { editStatus = false }) { Text("Cancelar") }
                            }
                        } else {
                            Text(userStatus, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Visible para otros: ${if (statusVisible) "Sí" else "No"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8899AA))
                        }
                    }
                    Switch(
                        checked = statusVisible,
                        onCheckedChange = {
                            statusVisible = it
                            prefs.edit().putBoolean("status_visible", it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CE6FF))
                    )
                    if (!editStatus) {
                        IconButton(onClick = { editStatus = true }) {
                            Icon(Icons.Filled.Edit, "Editar estado", modifier = Modifier.size(20.dp), tint = Color(0xFF4CE6FF))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

