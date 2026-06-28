package com.malla.mvp.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SmsFallbackSwitch(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("malla_prefs", Context.MODE_PRIVATE)
    var smsEnabled by remember { mutableStateOf(prefs.getBoolean("sms_fallback", false)) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF4CE6FF).copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Sms, null, tint = Color(0xFF4CE6FF), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("SMS como respaldo", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(
                if (smsEnabled) "Activado" else "Desactivado",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8899AA)
            )
        }
        Switch(
            checked = smsEnabled,
            onCheckedChange = { enabled ->
                smsEnabled = enabled
                prefs.edit().putBoolean("sms_fallback", enabled).apply()
            },
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CE6FF))
        )
    }
}
