package com.malla.mvp.ui.screen

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var fullScreenImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(conversationId) {
        vm.loadConversation(conversationId)
    }

    // Receptor de zumbido
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
                // Lista de mensajes
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState
                ) {
                    items(messages) { msg ->
                        MessageBubbleV2(msg = msg, animate = vm.isMessageNew(msg.timestamp), onImageClick = { uri -> fullScreenImageUri = uri })
                    }
                }

                // Barra inferior: cambia entre vista previa y composición normal
                if (pendingMediaUris.isNotEmpty()) {
                    // Barra de vista previa (WhatsApp-like)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Color(0xFF1A1A1A)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Fila de miniaturas con X para eliminar y botón + al final
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pendingMediaUris) { uri ->
                                    Box(
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Miniatura",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Botón X para eliminar
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .clickable { pendingMediaUris.remove(uri) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Eliminar",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                // Botón + para agregar más imágenes
                                item {
                                    IconButton(
                                        onClick = { showGalleryPanel = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Filled.Add, "Agregar más", tint = Color(0xFF4CE6FF))
                                    }
                                }
                            }
                            // Fila con caption, emoji y enviar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                OutlinedTextField(
                                    value = captionText,
                                    onValueChange = { captionText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Añade un pie de foto...", color = Color.Gray) },
                                    maxLines = 2,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                    leadingIcon = {
                                        IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                                            Icon(Icons.Filled.InsertEmoticon, "Emoji", tint = Color(0xFF4CE6FF))
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4CE6FF),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        // Enviar primera imagen con caption, las demás sin texto
                                        pendingMediaUris.forEachIndexed { index, uri ->
                                            val textToSend = if (index == 0 && captionText.isNotBlank()) captionText else ""
                                            vm.sendMessage(textToSend, mediaUri = uri.toString())
                                        }
                                        pendingMediaUris.clear()
                                        captionText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color(0xFF4CE6FF))
                                }
                            }
                        }
                    }
                } else {
                    // Barra de composición normal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Zumbido
                        IconButton(
                            onClick = {
                                if (!zumbidoCooldown) {
                                    vm.sendZumbido()
                                    zumbidoCooldown = true
                                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator?.vibrate(200)
                                    }
                                    coroutineScope.launch {
                                        repeat(3) {
                                            shakeOffset.animateTo(12f, animationSpec = tween(50))
                                            shakeOffset.animateTo(-12f, animationSpec = tween(50))
                                        }
                                        shakeOffset.animateTo(0f, animationSpec = tween(50))
                                    }
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
                                    IconButton(onClick = { /* TODO: cámara */ }) {
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
                            IconButton(
                                onClick = {
                                    if (text.isNotBlank()) {
                                        vm.sendMessage(text)
                                        text = ""
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color(0xFF4CE6FF))
                            }
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
                pendingMediaUris.clear()
                pendingMediaUris.addAll(uris)
                showGalleryPanel = false
            },
            initialSelected = pendingMediaUris.toList()
        )
    }
    // Diálogo de imagen a pantalla completa con zoom
    if (fullScreenImageUri != null) {
        Dialog(
            onDismissRequest = { fullScreenImageUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val scale = remember { Animatable(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val scope = rememberCoroutineScope()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenImageUri = null }  // Cerrar al tocar fondo
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale.value * zoom).coerceIn(0.5f, 5f)
                            scope.launch { scale.snapTo(newScale) }
                            offset = offset + pan
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scope.launch {
                                    if (scale.value > 1f) {
                                        scale.animateTo(1f, tween(200))
                                        offset = Offset.Zero
                                    } else {
                                        scale.animateTo(2f, tween(200))
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullScreenImageUri!!,
                    contentDescription = "Imagen ampliada",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

}

@Composable
fun MessageBubbleV2(msg: MessageData, animate: Boolean = false, onImageClick: (Uri) -> Unit = {}) {
    val isOwn = msg.isOwn
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
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Mostrar imagen si mediaUri existe, con click asegurado mediante Box
                if (msg.mediaUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(Uri.parse(msg.mediaUri)) }
                    ) {
                        AsyncImage(
                            model = Uri.parse(msg.mediaUri),
                            contentDescription = "Imagen enviada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Mostrar texto si no está vacío
                if (msg.content.isNotBlank()) {
                    Text(text = msg.content, color = Color.White, fontSize = 14.sp)
                }
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
