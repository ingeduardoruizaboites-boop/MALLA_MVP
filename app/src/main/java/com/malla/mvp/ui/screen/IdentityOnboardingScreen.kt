package com.malla.mvp.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.malla.mvp.core.crypto.*
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.UserIdentityEntity
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.ui.components.QrCodeDisplay
import kotlinx.coroutines.launch

@Composable
fun IdentityOnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()

    // Solo permisos peligrosos que requieren diálogo
    val dangerousPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    var permissionsGranted by remember {
        mutableStateOf(dangerousPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> permissionsGranted = dangerousPermissions.all { results[it] == true } }

    var currentStep by remember { mutableIntStateOf(if (permissionsGranted) 1 else 0) }
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && currentStep == 0) currentStep = 1
    }
    val totalSteps = 6

    var alias by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var generatedIdentity by remember { mutableStateOf<UserIdentityEntity?>(null) }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var inviteCode by remember { mutableStateOf<InviteCodeGenerator.InviteCode?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var creationError by remember { mutableStateOf<String?>(null) }

    val fadeAlpha = remember { Animatable(0f) }

    LaunchedEffect(currentStep) { fadeAlpha.animateTo(1f, animationSpec = tween(500)) }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0A1B2A), Color(0xFF0A1118)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).alpha(fadeAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                repeat(totalSteps) { step ->
                    Box(
                        modifier = Modifier.size(if (step == currentStep) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (step <= currentStep) Color(0xFF4CE6FF) else Color.White.copy(alpha = 0.3f))
                    )
                }
            }

            when (currentStep) {
                0 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Lock, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Permisos necesarios", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("MALLA necesita acceder a cámara y ubicación.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFB0BEC5), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { permissionLauncher.launch(dangerousPermissions) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                        ) { Text("Conceder permisos", fontWeight = FontWeight.Bold) }
                    }
                }
                1 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Shield, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "MALLA no necesita tu correo ni contraseña.\nTu número de teléfono es opcional y nunca sale de tu dispositivo.",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Tu identidad vive en este dispositivo.\nSolo tú puedes usarla.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFB0BEC5), textAlign = TextAlign.Center)
                    }
                }
                2 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Fingerprint, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Protege tu identidad", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Usa tu huella, rostro o PIN para\nblindar tu llave privada en el TEE.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFB0BEC5), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))
                        if (isCreating) {
                            CircularProgressIndicator(color = Color(0xFF4CE6FF))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Creando tu identidad única...", color = Color(0xFFB0BEC5))
                        } else {
                            Button(
                                onClick = {
                                    isCreating = true
                                    creationError = null
                                    scope.launch {
                                        try {
                                            val authSuccess = BiometricAuthHelper.authenticate(activity, "Protege tu identidad", "Usa tu huella, rostro o PIN")
                                            if (!authSuccess) {
                                                creationError = "Autenticación cancelada"
                                                Toast.makeText(context, "Autenticación cancelada. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                                                isCreating = false
                                                return@launch
                                            }
                                            val keystoreManager = KeystoreManager(context)
                                            val pubKeyBytes = keystoreManager.generateIdentityKey()
                                            val pubKeyBase64 = Base64.encodeToString(pubKeyBytes, Base64.NO_WRAP)
                                            val seal = IdentityProofOfWork.generateSeal(pubKeyBytes)
                                            val identicon = IdenticonGenerator.generate(pubKeyBytes)
                                            val entity = UserIdentityEntity(
                                                pubKeyBase64 = pubKeyBase64,
                                                pubKeyTruncated = pubKeyBase64.take(6) + "..." + pubKeyBase64.takeLast(6),
                                                displayName = alias.ifBlank { "Usuario MALLA" },
                                                avatarBase64 = null,
                                                identiconSvg = identicon,
                                                identitySealNonce = seal.nonce,
                                                identitySealTimestamp = seal.timestamp,
                                                identitySealHash = seal.hashHex,
                                                createdAt = System.currentTimeMillis(),
                                                isActive = true
                                            )
                                            AppDatabase.getInstance(context)?.identityDao()?.insertIdentity(entity)
                                            generatedIdentity = entity
                                            currentStep = 3
                                        } catch (e: Exception) {
                                            creationError = e.message ?: "Error desconocido"
                                        } finally {
                                            isCreating = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                            ) {
                                Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Activar protección", fontWeight = FontWeight.Bold)
                            }
                        }
                        creationError?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(it, color = Color(0xFFFF4C4C), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                3 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tu nombre de pantalla", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Puede ser tu nombre real o un apodo.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFB0BEC5), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = alias, onValueChange = { alias = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tu alias (opcional)", color = Color(0xFF8899AA)) },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CE6FF), unfocusedBorderColor = Color(0xFF8899AA))
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    AppDatabase.getInstance(context)?.identityDao()?.updateDisplayName(alias.ifBlank { "Usuario MALLA" })
                                    currentStep = 4
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                        ) { Text("Continuar", fontWeight = FontWeight.Bold) }
                    }
                }
                4 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tu número de teléfono (opcional)", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Solo lo usamos para encontrar a tus contactos. Nunca sale de tu dispositivo.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFB0BEC5), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("+52 1 55 1234 5678", color = Color(0xFF8899AA)) },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CE6FF), unfocusedBorderColor = Color(0xFF8899AA))
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { currentStep = 5 },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                        ) { Text("Continuar", fontWeight = FontWeight.Bold) }
                    }
                }
                5 -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.QrCode, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Tu identidad está lista", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Dirección: ${generatedIdentity?.pubKeyTruncated ?: ""}", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF4CE6FF), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))
                        if (qrPayload != null) {
                            Text("Comparte tu QR (válido 60s)", color = Color(0xFFB0BEC5))
                            Spacer(modifier = Modifier.height(8.dp))
                            QrCodeDisplay(content = qrPayload!!, size = 200)
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val authSuccess = BiometricAuthHelper.authenticate(activity, "Firma tu QR", "Confirma tu identidad")
                                            if (!authSuccess) {
                                                Toast.makeText(context, "Autenticación cancelada", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                            val keystoreManager = KeystoreManager(context)
                                            qrPayload = IdentityQrPayload.generate(keystoreManager, IdentityManager)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                            ) {
                                Icon(Icons.Filled.QrCode, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Mostrar QR efímero")
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        if (inviteCode != null) {
                            Text("Código de invitación (24h):", color = Color(0xFFB0BEC5))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(inviteCode!!.code.chunked(4).joinToString("-"), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp), color = Color(0xFF4CE6FF))
                        } else {
                            Button(
                                onClick = { inviteCode = InviteCodeGenerator.generate() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF).copy(alpha = 0.3f), contentColor = Color(0xFF4CE6FF))
                            ) {
                                Icon(Icons.Filled.Tag, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Generar código")
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onFinished,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                        ) { Text("Finalizar", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (currentStep > 1 && currentStep < 5) {
                    TextButton(onClick = { currentStep-- }) { Text("← Anterior", color = Color(0xFF8899AA)) }
                } else { Spacer(modifier = Modifier.width(1.dp)) }
                if (currentStep == 1) {
                    Button(
                        onClick = { currentStep = 2 },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                    ) { Text("Siguiente →", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
