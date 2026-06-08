package com.malla.mvp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.malla.mvp.core.crypto.InviteCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeInputScreen(
    onCodeValidated: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var isValidFormat by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    val validChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun validateCode(input: String): Boolean {
        if (input.length != 8) return false
        return input.all { it in validChars }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0A1B2A), Color(0xFF0A1118)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Botón volver
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Volver", tint = Color(0xFF8899AA))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                Icons.Filled.Tag,
                contentDescription = null,
                tint = Color(0xFF4CE6FF),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Ingresa el código de invitación",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Código de 8 caracteres alfanuméricos",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo de texto para el código
            OutlinedTextField(
                value = code,
                onValueChange = { input ->
                    val filtered = input.uppercase().filter { it in validChars }
                    if (filtered.length <= 8) {
                        code = filtered
                        isValidFormat = validateCode(filtered)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("MALLA-X7K2-P9WQ", color = Color(0xFF8899AA)) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF4CE6FF)
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CE6FF),
                    unfocusedBorderColor = if (isValidFormat || code.isEmpty()) Color(0xFF8899AA) else Color.Red
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                )
            )

            if (!isValidFormat && code.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "El código debe tener 8 caracteres válidos",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de validación
            Button(
                onClick = {
                    if (validateCode(code)) {
                        isProcessing = true
                        // Validar código (simulado por ahora)
                        val inviteCode = InviteCodeGenerator.InviteCode(code = code, expiresAt = System.currentTimeMillis() + 86400000)
                        if (InviteCodeGenerator.isValid(inviteCode)) {
                            onCodeValidated(code)
                        } else {
                            Toast.makeText(context, "Código inválido o expirado", Toast.LENGTH_SHORT).show()
                        }
                        isProcessing = false
                    } else {
                        Toast.makeText(context, "Formato de código incorrecto", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CE6FF),
                    contentColor = Color(0xFF0A1B2A)
                ),
                enabled = code.length == 8 && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color(0xFF0A1B2A), modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Validar código", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "El código es válido por 24 horas.\nCompártelo con tus contactos de forma segura.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8899AA),
                textAlign = TextAlign.Center
            )
        }
    }
}
