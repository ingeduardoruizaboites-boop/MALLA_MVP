package com.malla.mvp.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

    val avatarBitmap by IdentityManager.avatarBitmap.collectAsState()
    var userName by remember { mutableStateOf(IdentityManager.getUserName(context)) }
    var userStatus by remember { mutableStateOf(IdentityManager.getUserStatus(context)) }
    val phoneNumber = remember {
        context.getSharedPreferences("malla_prefs", android.content.Context.MODE_PRIVATE)
            .getString("phone", "Sin número") ?: "Sin número"
    }
    var bannerBitmap by remember { mutableStateOf(IdentityManager.loadBanner(context)) }

    val prefs = remember { context.getSharedPreferences("perfil_prefs", android.content.Context.MODE_PRIVATE) }
    var nameVisible by remember { mutableStateOf(prefs.getBoolean("name_visible", true)) }
    var statusVisible by remember { mutableStateOf(prefs.getBoolean("status_visible", true)) }
    var lastSeenVisible by remember { mutableStateOf(prefs.getBoolean("last_seen_visible", true)) }
    var readReceipts by remember { mutableStateOf(prefs.getBoolean("read_receipts", true)) }
    var messagePermission by remember { mutableStateOf(prefs.getString("message_permission", "todos") ?: "todos") }

    var qrPayload by remember { mutableStateOf<String?>(null) }
    var inviteCode by remember { mutableStateOf<InviteCodeGenerator.InviteCode?>(null) }
    var qrExpired by remember { mutableStateOf(false) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { IdentityManager.saveAvatar(context, it) }
    }
    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { bannerBitmap = IdentityManager.saveBanner(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1B2A), Color(0xFF0A1118))))
    ) {
        // Banner
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            if (bannerBitmap != null) {
                Image(
                    bitmap = bannerBitmap!!.asImageBitmap(),
                    contentDescription = "Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {}
            }
            IconButton(
                onClick = { bannerLauncher.launch("image/*") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            ) {
                Icon(Icons.Filled.Edit, "Cambiar banner", tint = Color.White)
            }
        }

        // Avatar grande (160 dp)
        Box(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.offset(y = (-80).dp).size(160.dp)) {
                Surface(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = CircleShape
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
                        .size(32.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        "Cambiar avatar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))

        // Nombre y teléfono reales
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = phoneNumber,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF8899AA),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── VISIBILIDAD Y PRIVACIDAD ──────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Privacidad", style = MaterialTheme.typography.titleSmall, color = Color(0xFF4CE6FF))
                Spacer(modifier = Modifier.height(12.dp))

                // Nombre visible
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar nombre", color = Color.White)
                    Switch(checked = nameVisible, onCheckedChange = { nameVisible = it; prefs.edit().putBoolean("name_visible", it).apply() }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CE6FF)))
                }
                // Estado visible
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar estado", color = Color.White)
                    Switch(checked = statusVisible, onCheckedChange = { statusVisible = it; prefs.edit().putBoolean("status_visible", it).apply() }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CE6FF)))
                }
                // Última conexión
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar última conexión", color = Color.White)
                    Switch(checked = lastSeenVisible, onCheckedChange = { lastSeenVisible = it; prefs.edit().putBoolean("last_seen_visible", it).apply() }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CE6FF)))
                }
                // Confirmación de lectura
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Confirmación de lectura", color = Color.White)
                    Switch(checked = readReceipts, onCheckedChange = { readReceipts = it; prefs.edit().putBoolean("read_receipts", it).apply() }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CE6FF)))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Quién puede enviarme mensajes", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos" to "todos", "Contactos" to "contactos", "Nadie" to "nadie").forEach { (label, value) ->
                        FilterChip(
                            selected = messagePermission == value,
                            onClick = { messagePermission = value; prefs.edit().putString("message_permission", value).apply() },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF4CE6FF))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── QR Y CÓDIGO (EN FILA) ──────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tarjeta QR
            Card(
                modifier = Modifier.weight(1f).clickable {
                    scope.launch {
                        val authSuccess = com.malla.mvp.core.crypto.BiometricAuthHelper.authenticate(
                            context as androidx.fragment.app.FragmentActivity,
                            "Firma tu QR",
                            "Confirma tu identidad para mostrar el QR"
                        )
                        if (!authSuccess) {
                            Toast.makeText(context, "Autenticación cancelada", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        try {
                            val keystoreManager = KeystoreManager(context)
                            qrPayload = IdentityQrPayload.generate(keystoreManager, IdentityManager)
                            qrExpired = false
                            kotlinx.coroutines.delay(60_000L)
                            qrExpired = true
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3B))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.QrCode, "QR", tint = Color(0xFF4CE6FF), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("QR efímero", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text("60 segundos", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8899AA))
                    if (qrPayload != null && !qrExpired) {
                        QrCodeDisplay(content = qrPayload!!, size = 120)
                    }
                }
            }
            // Tarjeta Código
            Card(
                modifier = Modifier.weight(1f).clickable { inviteCode = InviteCodeGenerator.generate() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2C3B))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Tag, "Código", tint = Color(0xFF4CE6FF), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Código", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text("24 horas", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8899AA))
                    if (inviteCode != null && InviteCodeGenerator.isValid(inviteCode)) {
                        Text(
                            inviteCode!!.code.chunked(4).joinToString("-"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4CE6FF)
                        )
                    } else if (inviteCode != null) {
                        Text("Expirado", color = Color(0xFFFF4C4C))
                    }
                }
            }
        }

        // Mostrar QR expandido si existe
        if (qrPayload != null && !qrExpired) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text("Toca para compartir", modifier = Modifier.padding(16.dp), color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
