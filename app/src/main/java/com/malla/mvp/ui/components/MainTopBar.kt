package com.malla.mvp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(onSettingsClick: () -> Unit, isOnline: Boolean) {
    TopAppBar(
        title = {
            Text(
                text = "M A L L A",
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menú")
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
