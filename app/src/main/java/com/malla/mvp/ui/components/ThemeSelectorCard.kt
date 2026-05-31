package com.malla.mvp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malla.mvp.viewmodel.AppThemeState
import com.malla.mvp.viewmodel.ThemeColors

@Composable
fun ThemeSelectorCard(
    currentTheme: ThemeColors,
    onThemeSelected: (ThemeColors) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tema de color", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            AppThemeState.availableThemes.forEach { theme ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme.name == theme.name,
                        onClick = { onThemeSelected(theme) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(theme.name)
                }
            }
        }
    }
}
