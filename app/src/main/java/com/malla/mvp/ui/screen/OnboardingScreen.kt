package com.malla.mvp.ui.screen

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val permissionsToRequest = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var allGranted by remember {
        mutableStateOf(permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> allGranted = results.values.all { it } }

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    // Animación de entrada
    val slideOffset = remember { Animatable(50f) }
    val fadeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        slideOffset.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        fadeAlpha.animateTo(1f, animationSpec = tween(800))
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF0A1B2A), Color(0xFF112233), Color(0xFF0A1B2A))
            )
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).alpha(fadeAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Indicador de paso
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                repeat(totalSteps) { step ->
                    Box(
                        modifier = Modifier.size(if (step == currentStep) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (step == currentStep) Color(0xFF00FFFF) else Color.White.copy(alpha = 0.3f)))
                }
            }

            // Contenido animado por paso
            AnimatedContent(targetState = currentStep, transitionSpec = {
                (slideInVertically(tween(500)) { -it } + fadeIn(tween(500))).togetherWith(slideOutVertically(tween(500)) { it } + fadeOut(tween(500)))
            }, label = "step") { step ->
                when (step) {
                    0 -> StepContent(
                        icon = Icons.Filled.WifiTethering,
                        title = "Sin Internet, Sin Problemas",
                        description = "MALLA crea redes locales usando Bluetooth y WiFi Direct para mantenerte comunicado donde otros fallan."
                    )
                    1 -> StepContent(
                        icon = Icons.Filled.Security,
                        title = "Privacidad Total",
                        description = "Cifrado de extremo a extremo. Sin servidores, sin cuentas, sin rastreo. Tus conversaciones te pertenecen."
                    )
                    2 -> StepContent(
                        icon = Icons.Filled.Bolt,
                        title = "Listo para la Acción",
                        description = "Concede los permisos necesarios y únete a la red más resistente de LATAM."
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botones inferiores
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF), contentColor = Color(0xFF0A1B2A))
                    ) {
                        Text("Siguiente →", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Último paso: solicitar permisos o continuar
                    if (allGranted) {
                        Button(
                            onClick = onPermissionsGranted,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF), contentColor = Color(0xFF0A1B2A))
                        ) {
                            Text("COMENZAR", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { launcher.launch(permissionsToRequest) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF), contentColor = Color(0xFF0A1B2A))
                        ) {
                            Icon(Icons.Filled.Lock, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("CONCEDER PERMISOS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepContent(icon: ImageVector, title: String, description: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00FFFF),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center
        )
    }
}
