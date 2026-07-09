package com.malla.mvp.ui.screen

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.malla.mvp.core.data.MessageData
import com.malla.mvp.events.MallaEventBus
import com.malla.mvp.ui.components.GalleryPickerPanel
import com.malla.mvp.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    contactName: String,
    isMeshMode: Boolean = false,
    onBack: () -> Unit = {},
    onProfileClicked: () -> Unit = {},
    onVoiceCallClick: () -> Unit = {},
    onVideoCallClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: ChatViewModel = viewModel()
    val messages by vm.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showGalleryPanel by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var zumbidoCooldown by remember { mutableStateOf(false) }
    val pendingMediaUris = remember { mutableStateListOf<Uri>() }
    var captionText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(conversationId) {
        vm.loadConversation(conversationId)
    }

    // Receptor de zumbido (shake global)
    LaunchedEffect(Unit) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        MallaEventBus.zumbidoReceived.collect { msg ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
            repeat(3) {
                shakeOffset.animateTo(12f, animationSpec = tween(50))
                shakeOffset.animateTo(-12f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50))
        }
    }

    LaunchedEffect(zumbidoCooldown) {
        if (zumbidoCooldown) {
            delay(5000)
            zumbidoCooldown = false
        }
    }

    // Contenedor que vibra completo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = shakeOffset.value }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(contactName, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A1B2A))
                )
            },
            containerColor = Color(0xFF0A1118)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Vista previa de imágenes
                if (pendingMediaUris.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Color(0xFF1A1A1A)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pendingMediaUris) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Miniatura",
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                OutlinedTextField(
                                    value = captionText,
                                    onValueChange = { captionText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Añade un pie de foto...", color = Color.Gray) },
                                    maxLines = 2,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4CE6FF),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        if (captionText.isNotBlank()) vm.sendMessage(captionText)
                                        pendingMediaUris.forEach { uri -> vm.sendMessage("[Imagen: $uri]") }
                                        pendingMediaUris.clear()
                                        captionText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color(0xFF4CE6FF))
                                }
                            }
                        }
                    }
                }

                // Lista de mensajes
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState
                ) {
                    items(messages) { msg ->
                        MessageBubbleV2(msg = msg, animate = vm.isMessageNew(msg.timestamp))
                    }
                }

                // Barra de composición premium
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Botón de zumbido
                    IconButton(
                        onClick = {
                            if (!zumbidoCooldown) {
                                vm.sendZumbido()
                                zumbidoCooldown = true

                                // Vibración local
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(200)
                                }
                                // Shake local
                                coroutineScope.launch {
                                    repeat(3) {
                                        shakeOffset.animateTo(12f, animationSpec = tween(50))
                                        shakeOffset.animateTo(-12f, animationSpec = tween(50))
                                    }
                                    shakeOffset.animateTo(0f, animationSpec = tween(50))
                                }
                                // Sonido
                                try {
                                    val mp = android.media.MediaPlayer.create(context, com.malla.mvp.R.raw.zumbido)
                                    mp?.start()
                                    mp?.setOnCompletionListener { it.release() }
                                } catch (_: Exception) {}
                            }
                        },
                        enabled = !zumbidoCooldown
                    ) {
                        Icon(
                            Icons.Filled.Vibration,
                            "Zumbido",
                            tint = if (zumbidoCooldown) Color.Gray else Color(0xFF4CE6FF)
                        )
                    }

                    // Campo de texto
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mensaje", color = Color.Gray) },
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CE6FF),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedContainerColor = Color(0xFF1A2A3A),
                            unfocusedContainerColor = Color(0xFF1A2A3A)
                        ),
                        leadingIcon = {
                            IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                                Icon(Icons.Filled.InsertEmoticon, "Emoji", tint = Color(0xFF4CE6FF))
                            }
                        },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showAttachmentSheet = true }) {
                                    Icon(Icons.Filled.AttachFile, "Adjuntar", tint = Color(0xFF4CE6FF))
                                }
                                IconButton(onClick = { showAttachmentSheet = true }) {
                                    Icon(Icons.Filled.CameraAlt, "Cámara", tint = Color(0xFF4CE6FF))
                                }
                            }
                        }
                    )

                    // Micrófono / Enviar
                    if (text.isBlank()) {
                        IconButton(onClick = { /* TODO: grabar nota de voz */ }) {
                            Icon(Icons.Filled.Mic, "Grabar", tint = Color(0xFF4CE6FF))
                        }
                    } else {
                        IconButton(onClick = {
                            if (text.isNotBlank()) {
                                vm.sendMessage(text)
                                text = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color(0xFF4CE6FF))
                        }
                    }
                }

                // Panel de emojis
                if (showEmojiPicker) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A38))
                    ) {
                        val emojis = listOf("😀","😂","😍","😢","😡","👍","👋","🎉","❤️","🔥","😎","🙏","💪","🤔","😴","🥳")
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                                emojis.take(8).forEach { emoji ->
                                    Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(4.dp).clickable {
                                        text = text + emoji; showEmojiPicker = false
                                    })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                                emojis.drop(8).forEach { emoji ->
                                    Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(4.dp).clickable {
                                        text = text + emoji; showEmojiPicker = false
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Paneles externos (no se mueven con el shake)
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Enviar multimedia", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showAttachmentSheet = false; showGalleryPanel = true }) {
                            Icon(Icons.Filled.Photo, "Galería", tint = Color(0xFF4CE6FF))
                        }
                        Text("Galería", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* TODO: cámara */ }) {
                            Icon(Icons.Filled.CameraAlt, "Cámara", tint = Color(0xFF4CE6FF))
                        }
                        Text("Cámara", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* TODO: documento */ }) {
                            Icon(Icons.Filled.InsertDriveFile, "Documento", tint = Color(0xFF4CE6FF))
                        }
                        Text("Documento", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* TODO: ubicación */ }) {
                            Icon(Icons.Filled.LocationOn, "Ubicación", tint = Color(0xFF4CE6FF))
                        }
                        Text("Ubicación", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* TODO: contacto */ }) {
                            Icon(Icons.Filled.PersonAdd, "Contacto", tint = Color(0xFF4CE6FF))
                        }
                        Text("Contacto", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* TODO: encuesta */ }) {
                            Icon(Icons.Filled.Poll, "Encuesta", tint = Color(0xFF4CE6FF))
                        }
                        Text("Encuesta", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }

    if (showGalleryPanel) {
        GalleryPickerPanel(
            onDismiss = { showGalleryPanel = false },
            onConfirm = { uris ->
                pendingMediaUris.addAll(uris)
                showGalleryPanel = false
            }
        )
    }
}

@Composable
fun MessageBubbleV2(msg: MessageData, animate: Boolean = false) {
    val isOwn = msg.isOwn
    val align = if (isOwn) Alignment.End else Alignment.Start
    val bgColor = if (isOwn) Color(0xFF1A3B4A) else Color(0xFF2A2A2A)
    val scaleAnim = if (animate) {
        val anim = remember { Animatable(0.8f) }
        LaunchedEffect(msg.id) {
            anim.animateTo(
                1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        anim.value
    } else 1f

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).scale(scaleAnim),
        horizontalAlignment = align
    ) {
        Surface(color = bgColor, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = msg.content, color = Color.White, fontSize = 14.sp)
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
