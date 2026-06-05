package com.malla.mvp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.malla.mvp.ui.theme.MallaColorScheme

@Composable
fun ThemeSelectorCard(
    currentScheme: MallaColorScheme,
    onSchemeSelected: (MallaColorScheme) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tema de color", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MallaColorScheme.ALL.size) { index ->
                    val scheme = MallaColorScheme.ALL[index]
                    val isSelected = currentScheme.name == scheme.name
                    ThemeCircle(
                        scheme = scheme,
                        isSelected = isSelected,
                        onClick = { onSchemeSelected(scheme) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeCircle(
    scheme: MallaColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(scheme.primary)
                .then(
                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = scheme.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
}
