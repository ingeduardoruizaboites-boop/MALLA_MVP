package com.malla.mvp.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malla.mvp.ui.settings.AccessibilitySettings
import com.malla.mvp.ui.settings.BubbleStyle
import com.malla.mvp.ui.settings.ChatSettings
import com.malla.mvp.ui.components.BubbleShapes
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var fontSize by remember { mutableStateOf(ChatSettings.fontSize.value) }
    var opacity by remember { mutableStateOf(ChatSettings.bubbleOpacity.value) }

    // Lecturas reactivas directas sin delegados
    val currentBubbleStyle = AccessibilitySettings.bubbleStyle.collectAsState().value
    val ownBubbleColor = AccessibilitySettings.ownBubbleColor.collectAsState().value
    val otherBubbleColor = AccessibilitySettings.otherBubbleColor.collectAsState().value
    val ownTextColor = ChatSettings.ownTextColor.collectAsState().value
    val otherTextColor = ChatSettings.otherTextColor.collectAsState().value

    val previewText = "¡Hola! Así se verá tu mensaje."
    val ownColor = ownBubbleColor ?: Color(0xFF1A3B4A)
    val otherColor = otherBubbleColor ?: Color(0xFF2A2A2A)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de chat", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A1B2A))
            )
        },
        containerColor = Color(0xFF0A1118)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Vista previa
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A3A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Vista previa", style = MaterialTheme.typography.titleSmall, color = Color(0xFF4CE6FF), modifier = Modifier.padding(bottom = 12.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Surface(
                            color = otherColor,
                            shape = BubbleShapes.getShape(currentBubbleStyle, false),
                            shadowElevation = 4.dp,
                            modifier = Modifier.widthIn(max = 260.dp).alpha(opacity)
                        ) {
                            Text(
                                text = "Mensaje recibido",
                                color = otherTextColor ?: contrastingTextColor(otherColor),
                                fontSize = fontSize.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Surface(
                            color = ownColor,
                            shape = BubbleShapes.getShape(currentBubbleStyle, true),
                            shadowElevation = 4.dp,
                            modifier = Modifier.widthIn(max = 260.dp).alpha(opacity)
                        ) {
                            Text(
                                text = previewText,
                                color = ownTextColor ?: contrastingTextColor(ownColor),
                                fontSize = fontSize.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Tamaño del texto
            SectionCard(title = "Tamaño del texto") {
                Text("Define qué tan grande se verá la letra.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { fontSize = (fontSize - 1).coerceIn(10f, 22f); ChatSettings.updateFontSize(fontSize, context) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Remove, "Reducir", tint = Color(0xFF4CE6FF))
                    }
                    Slider(
                        value = fontSize, onValueChange = { fontSize = it }, valueRange = 10f..22f, steps = 11,
                        onValueChangeFinished = { ChatSettings.updateFontSize(fontSize, context) },
                        modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFF4CE6FF), activeTrackColor = Color(0xFF4CE6FF))
                    )
                    IconButton(onClick = { fontSize = (fontSize + 1).coerceIn(10f, 22f); ChatSettings.updateFontSize(fontSize, context) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, "Aumentar", tint = Color(0xFF4CE6FF))
                    }
                }
                Text("${fontSize.roundToInt()} sp", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Transparencia
            SectionCard(title = "Transparencia de burbujas") {
                Text("Ajusta la opacidad.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { opacity = (opacity - 0.05f).coerceIn(0.5f, 1.0f); ChatSettings.updateBubbleOpacity(opacity, context) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Remove, "Reducir", tint = Color(0xFF4CE6FF))
                    }
                    Slider(
                        value = opacity, onValueChange = { opacity = it }, valueRange = 0.5f..1.0f,
                        onValueChangeFinished = { ChatSettings.updateBubbleOpacity(opacity, context) },
                        modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color(0xFF4CE6FF), activeTrackColor = Color(0xFF4CE6FF))
                    )
                    IconButton(onClick = { opacity = (opacity + 0.05f).coerceIn(0.5f, 1.0f); ChatSettings.updateBubbleOpacity(opacity, context) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, "Aumentar", tint = Color(0xFF4CE6FF))
                    }
                }
                Text("${(opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Color de burbujas propias
            SectionCard(title = "Color de burbujas propias") {
                ColorPickerRow(
                    colors = listOf(null to "Tema", Color(0xFF00E5FF) to "Cyan", Color(0xFF4CAF50) to "Verde", Color(0xFFFF7043) to "Naranja", Color(0xFF9575CD) to "Morado", Color(0xFF78909C) to "Gris"),
                    currentColor = ownBubbleColor,
                    onColorSelected = { color -> AccessibilitySettings.ownBubbleColor.value = color; AccessibilitySettings.save(context) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionCard(title = "Color de burbujas del contacto") {
                ColorPickerRow(
                    colors = listOf(null to "Tema", Color(0xFF00E5FF) to "Cyan", Color(0xFF4CAF50) to "Verde", Color(0xFFFF7043) to "Naranja", Color(0xFF9575CD) to "Morado", Color(0xFF78909C) to "Gris"),
                    currentColor = otherBubbleColor,
                    onColorSelected = { color -> AccessibilitySettings.otherBubbleColor.value = color; AccessibilitySettings.save(context) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Estilo de burbujas
            SectionCard(title = "Estilo de burbujas") {
                Text("Elige la forma que más te guste.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                BubbleStyleSelectorWithPreview(
                    currentStyle = currentBubbleStyle,
                    onStyleSelected = { style -> AccessibilitySettings.bubbleStyle.value = style; AccessibilitySettings.save(context) }
                )
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15202B)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

@Composable
fun ColorPickerRow(colors: List<Pair<Color?, String>>, currentColor: Color?, onColorSelected: (Color?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        colors.forEach { (color, _) ->
            val isSelected = currentColor == color
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(color ?: Color.Gray)
                    .then(if (isSelected) Modifier.border(3.dp, Color(0xFF4CE6FF), CircleShape) else Modifier)
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Filled.Check, "Seleccionado", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun BubbleStyleSelectorWithPreview(currentStyle: BubbleStyle, onStyleSelected: (BubbleStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BubbleStyle.values().forEach { style ->
            val isSelected = currentStyle == style
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onStyleSelected(style) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF4CE6FF).copy(alpha = 0.15f) else Color.Transparent),
                border = if (isSelected) BorderStroke(1.dp, Color(0xFF4CE6FF)) else null
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Surface(color = Color(0xFF1A3B4A), shape = BubbleShapes.getShape(style, true), shadowElevation = 1.dp) {
                            Spacer(modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = style.label, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) Color.White else Color.LightGray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        Text(text = styleDescription(style), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (isSelected) Icon(Icons.Filled.CheckCircle, "Seleccionado", tint = Color(0xFF4CE6FF), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

fun styleDescription(style: BubbleStyle): String = when (style) {
    BubbleStyle.MODERN -> "Suave y moderno"
    BubbleStyle.ROUNDED -> "Clásico redondeado"
    BubbleStyle.COMIC -> "Divertido y expresivo"
    BubbleStyle.PIXEL -> "Estilo retro pixelado"
    BubbleStyle.COLA -> "Con cola de burbuja"
}


fun contrastingTextColor(bgColor: Color): Color {
    val luminance = 0.2126f * bgColor.red + 0.7152f * bgColor.green + 0.0722f * bgColor.blue
    return if (luminance > 0.5f) Color.Black else Color.White
}
