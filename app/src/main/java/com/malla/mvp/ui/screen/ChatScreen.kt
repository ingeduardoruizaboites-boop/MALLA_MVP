package com.malla.mvp.ui.screen

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.location.LocationManager
import android.location.Location
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.alpha
import com.malla.mvp.ui.settings.ChatSettings
import androidx.compose.ui.graphics.Brush
import com.malla.mvp.ui.components.BubbleShapes
import com.malla.mvp.ui.settings.AccessibilitySettings
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
import com.malla.mvp.ui.components.VoiceRecorderButton
import com.malla.mvp.viewmodel.ChatViewModel
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import com.malla.mvp.ui.components.AudioBubblePlayer
import com.malla.mvp.media.VoiceRecorder
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
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val voiceRecorder = remember { VoiceRecorder(context) }

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
                    actions = {
                        IconButton(onClick = onProfileClicked) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = contactName.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    )
                                }
                            }
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
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            placeholder = { Text("Mensaje", color = Color.Gray) },
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 16.sp),
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
                                Row(
                                    modifier = Modifier.animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                                ) {
                                    IconButton(onClick = { showAttachmentSheet = true }) {
                                        Icon(Icons.Filled.AttachFile, "Adjuntar", tint = Color(0xFF4CE6FF))
                                    }
                                    AnimatedVisibility(
                                        visible = text.isBlank(),
                                        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
                                        exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                                    ) {
                                        IconButton(onClick = { /* TODO: cámara */ }) {
                                            Icon(Icons.Filled.CameraAlt, "Cámara", tint = Color(0xFF4CE6FF))
                                        }
                                    }
                                }
                            }
                        )

                        // Micrófono / Enviar
                        if (text.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color.Red.copy(alpha = 0.2f) else Color.Transparent)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitFirstDown()
                                                try {
                                                    // Verificar permiso
                                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                                            context,
                                                            android.Manifest.permission.RECORD_AUDIO
                                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    ) {
                                                        android.widget.Toast.makeText(context, "Permiso de micrófono requerido", android.widget.Toast.LENGTH_SHORT).show()
                                                        return@awaitPointerEventScope
                                                    }
                                                    val file = voiceRecorder.startRecording()
                                                    if (file == null) {
                                                        android.widget.Toast.makeText(context, "Error al iniciar grabación", android.widget.Toast.LENGTH_SHORT).show()
                                                        return@awaitPointerEventScope
                                                    }
                                                    isRecording = true
                                                    elapsedSeconds = 0
                                                    val timerJob = coroutineScope.launch {
                                                        while (isRecording) {
                                                            delay(1000)
                                                            elapsedSeconds++
                                                        }
                                                    }
                                                    waitForUpOrCancellation()
                                                    timerJob.cancel()
                                                    val recordedFile = try {
                                                        voiceRecorder.stopRecording()
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Error al detener: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                        null
                                                    }
                                                    isRecording = false
                                                    elapsedSeconds = 0
                                                    if (recordedFile != null && recordedFile.length() > 0) {
                                                        vm.sendMessage("", mediaUri = recordedFile.absolutePath)
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Audio vacío o no disponible", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Error inesperado: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                    isRecording = false
                                                    elapsedSeconds = 0
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRecording) {
                                    // Animación de pulsación
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val pulseScale by infiniteTransition.animateFloat(
                                        initialValue = 1f, targetValue = 1.3f,
                                        animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse),
                                        label = "pulse"
                                    )
                                    Icon(Icons.Filled.Mic, "Grabando", tint = Color.Red, modifier = Modifier.size(24.dp).scale(pulseScale))
                                } else {
                                    Icon(Icons.Filled.Mic, "Grabar", tint = Color(0xFF4CE6FF))
                                }
                            }
                            if (isRecording) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatSeconds(elapsedSeconds),
                                    color = Color.Red.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
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

                                // Indicador de grabación estilo WhatsApp
                if (isRecording) {
                    val amp by voiceRecorder.amplitude.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.weight(1f).height(24.dp)) {
                            val barCount = 20
                            val barWidth = size.width / barCount * 0.6f
                            val spacing = size.width / barCount * 0.4f
                            val maxBarHeight = size.height
                            val normalizedAmp = (amp / 32767f).coerceIn(0.01f, 1f)
                            for (i in 0 until barCount) {
                                val fraction = (i.toFloat() / barCount)
                                val barHeight = maxBarHeight * normalizedAmp * (0.3f + 0.7f * fraction)
                                drawRoundRect(
                                    color = Color(0xFF4CE6FF),
                                    topLeft = Offset(i * (barWidth + spacing), maxBarHeight - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatSeconds(elapsedSeconds), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
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
                        IconButton(onClick = {
                            showAttachmentSheet = false
                            val loc = getBestLocation(context)
                            if (loc != null) {
                                val lat = loc.latitude; val lon = loc.longitude
                                vm.sendMessage("📍 Ubicación actual\nhttps://maps.google.com/maps?q=$lat,$lon")
                            } else {
                                vm.sendMessage("📍 Ubicación no disponible. Concede permisos de ubicación.")
                            }
                        }) {
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
    val bubbleStyle by AccessibilitySettings.bubbleStyle.collectAsState()
    val ownBubbleColor by AccessibilitySettings.ownBubbleColor.collectAsState()
    val otherBubbleColor by AccessibilitySettings.otherBubbleColor.collectAsState()
    val baseColor = (if (isOwn) ownBubbleColor else otherBubbleColor)
        ?: if (isOwn) Color(0xFF1A3B4A) else Color(0xFF2A2A2A)
    val ownTextColor by ChatSettings.ownTextColor.collectAsState()
    val otherTextColor by ChatSettings.otherTextColor.collectAsState()
    val textColor = (if (isOwn) ownTextColor else otherTextColor)
        ?: contrastingTextColor(baseColor)
    val bubbleOpacity by ChatSettings.bubbleOpacity.collectAsState()
    val fontSize by ChatSettings.fontSize.collectAsState()

    // Animación Apple-style
    val scaleAnim = if (animate) {
        val anim = remember { Animatable(0.85f) }
        LaunchedEffect(msg.id) {
            anim.animateTo(1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 1500f))
        }
        anim.value
    } else 1f

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = BubbleShapes.getShape(bubbleStyle, isOwn),
            shadowElevation = 4.dp,
            modifier = Modifier
                .widthIn(min = 100.dp, max = 270.dp)
                .graphicsLayer {
                    scaleX = scaleAnim; scaleY = scaleAnim
                    transformOrigin = if (isOwn) TransformOrigin(1f, 1f) else TransformOrigin(0f, 1f)
                }
                .alpha(bubbleOpacity),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(listOf(baseColor.lighten(0.15f), baseColor)),
                    shape = BubbleShapes.getShape(bubbleStyle, isOwn)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (msg.mediaUri != null) {
                        val uri = Uri.parse(msg.mediaUri)
                        if (msg.mediaUri.endsWith(".3gp") || msg.mediaUri.endsWith(".m4a") || msg.mediaUri.contains("voice_")) {
                            AudioBubblePlayer(filePath = msg.mediaUri, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)).clickable { onImageClick(uri) }
                            ) {
                                AsyncImage(model = uri, contentDescription = "Imagen enviada", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    if (msg.content.isNotBlank()) {
                        Text(text = msg.content, color = textColor, fontSize = fontSize.sp)
                    }
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}


fun Color.lighten(factor: Float = 0.1f): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}


fun getBestLocation(context: Context): Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return null
    }
    var best: Location? = null
    try { best = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) {}
    if (best == null) try { best = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) {}
    if (best == null) try { best = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) {}
    return best
}


fun formatSeconds(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}
