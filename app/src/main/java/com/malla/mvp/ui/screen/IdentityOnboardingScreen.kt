package com.malla.mvp.ui.screen

import android.util.Base64
import android.widget.Toast
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.core.crypto.*
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.ContactEntity
import com.malla.mvp.data.entity.ContactStatus
import com.malla.mvp.data.entity.UserIdentityEntity
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.ui.components.QrCodeDisplay
import kotlinx.coroutines.launch

@Composable
fun IdentityOnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    // Estado
    var alias by remember { mutableStateOf("") }
    var generatedIdentity by remember { mutableStateOf<UserIdentityEntity?>(null) }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var inviteCode by remember { mutableStateOf<InviteCodeGenerator.InviteCode?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var creationError by remember { mutableStateOf<String?>(null) }

    val fadeAlpha = remember { Animatable(0f) }
    val slideOffset = remember { Animatable(50f) }

    LaunchedEffect(currentStep) {
        fadeAlpha.animateTo(1f, animationSpec = tween(500))
        slideOffset.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
    }

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
            // Indicador de paso
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                repeat(totalSteps) { step ->
                    Box(
                        modifier = Modifier.size(if (step == currentStep) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (step <= currentStep) Color(0xFF4CE6FF) else Color.White.copy(alpha = 0.3f))
                    )
                }
            }

            when (currentStep) {
                0 -> StepWelcome()
                1 -> StepBiometricSetup(
                    isCreating = isCreating,
                    creationError = creationError,
                    onCreateIdentity = {
                        isCreating = true
                        creationError = null
                        scope.launch {
                            try {
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
                                currentStep = 2
                            } catch (e: Exception) {
                                creationError = e.message ?: "Error desconocido"
                            } finally {
                                isCreating = false
                            }
                        }
                    }
                )
                2 -> StepAliasAvatar(
                    alias = alias,
                    onAliasChange = { alias = it },
                    identicon = generatedIdentity?.identiconSvg,
                    onContinue = {
                        scope.launch {
                            AppDatabase.getInstance(context)?.identityDao()?.updateDisplayName(alias.ifBlank { "Usuario MALLA" })
                            currentStep = 3
                        }
                    }
                )
                3 -> StepShareIdentity(
                    pubKeyTruncated = generatedIdentity?.pubKeyTruncated ?: "",
                    qrPayload = qrPayload,
                    inviteCode = inviteCode?.code,
                    onGenerateQr = {
                        scope.launch {
                            try {
                                val keystoreManager = KeystoreManager(context)
                                qrPayload = IdentityQrPayload.generate(keystoreManager, IdentityManager)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onGenerateCode = {
                        inviteCode = InviteCodeGenerator.generate()
                    },
                    onFinish = { onFinished() }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Navegación
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("← Anterior", color = Color(0xFF8899AA))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (currentStep < totalSteps - 1) {
                    Button(
                        onClick = { currentStep++ },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
                    ) {
                        Text("Siguiente →", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Shield, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "MALLA no necesita tu correo,\nteléfono ni contraseña.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Tu identidad vive en este dispositivo.\nSolo tú puedes usarla.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepBiometricSetup(
    isCreating: Boolean,
    creationError: String?,
    onCreateIdentity: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Fingerprint, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Protege tu identidad",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Usa tu huella, rostro o PIN para\nblindar tu llave privada en el TEE.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (isCreating) {
            CircularProgressIndicator(color = Color(0xFF4CE6FF))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Creando tu identidad única...", color = Color(0xFFB0BEC5))
        } else {
            Button(
                onClick = onCreateIdentity,
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

@Composable
private fun StepAliasAvatar(
    alias: String,
    onAliasChange: (String) -> Unit,
    identicon: String?,
    onContinue: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Tu nombre de pantalla",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Puede ser tu nombre real o un apodo.\nPuedes cambiarlo cuando quieras.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = alias,
            onValueChange = onAliasChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tu alias (opcional)", color = Color(0xFF8899AA)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CE6FF),
                unfocusedBorderColor = Color(0xFF8899AA)
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
        ) {
            Text("Continuar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepShareIdentity(
    pubKeyTruncated: String,
    qrPayload: String?,
    inviteCode: String?,
    onGenerateQr: () -> Unit,
    onGenerateCode: () -> Unit,
    onFinish: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.QrCode, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(72.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Tu identidad está lista",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Dirección: $pubKeyTruncated",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF4CE6FF),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // QR
        if (qrPayload != null) {
            Text("Comparte tu QR (válido 60s)", color = Color(0xFFB0BEC5))
            Spacer(modifier = Modifier.height(8.dp))
            QrCodeDisplay(content = qrPayload, size = 200)
        } else {
            Button(
                onClick = onGenerateQr,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
            ) {
                Icon(Icons.Filled.QrCode, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mostrar QR efímero")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Código de 8 caracteres
        if (inviteCode != null) {
            Text("Código de invitación (24h):", color = Color(0xFFB0BEC5))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                inviteCode.chunked(4).joinToString("-"),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                color = Color(0xFF4CE6FF)
            )
        } else {
            Button(
                onClick = onGenerateCode,
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
            onClick = onFinish,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CE6FF), contentColor = Color(0xFF0A1B2A))
        ) {
            Text("Finalizar", fontWeight = FontWeight.Bold)
        }
    }
}
