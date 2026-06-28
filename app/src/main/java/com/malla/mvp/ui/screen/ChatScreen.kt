package com.malla.mvp.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import coil.compose.AsyncImage
import com.malla.mvp.identity.IdentityManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import com.malla.mvp.R
import com.malla.mvp.data.AppDatabase
import com.malla.mvp.data.entity.ConversationEntity
import com.malla.mvp.data.entity.MessageEntity
import com.malla.mvp.network.MeshMessage
import com.malla.mvp.data.entity.PollEntity
import com.malla.mvp.data.entity.PollOptionEntity
import com.malla.mvp.network.NetworkService
import com.malla.mvp.ui.settings.AccessibilitySettings
import com.malla.mvp.ui.settings.BubbleStyle
import com.malla.mvp.ui.components.BubbleShapes
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import org.json.JSONObject
import org.json.JSONArray
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    conversationId: String,
    contactName: String,
    isMeshMode: Boolean = false,
    onBack: () -> Unit = {},
    onProfileClicked: () -> Unit = {}, onVoiceCallClick: () -> Unit = {}, onVideoCallClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    DisposableEffect(viewModel) {
        StickerState.onSendSticker = { url -> viewModel?.sendImage(url) }
        onDispose { StickerState.onSendSticker = null }
    }
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf(emptyList<MessageEntity>()) }
    var text by remember { mutableStateOf("") }
    var searchMessageQuery by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showEphemeralMenu by remember { mutableStateOf(false) }
    var showCreatePollDialog by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var ephemeralDuration by remember { mutableStateOf<Long?>(null) }
    var viewOnce by remember { mutableStateOf(false) }
    var isFiestaMode by remember { mutableStateOf(false) }
    var chatBackground by remember { mutableStateOf(-1) }
    var gradientType by remember { mutableStateOf(0) }
    var backgroundImageUri by remember { mutableStateOf<Uri?>(null) }
    var conversationBgColor by remember { mutableStateOf<Int?>(null) }
    var shakeOffset by remember { mutableStateOf(Animatable(0f)) }
    var hasCustomRingtone by remember { mutableStateOf(false) }
    var showImageEditor by remember { mutableStateOf(false) }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var editorText by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var polls by remember { mutableStateOf(emptyList<PollEntity>()) }
    var optionsMap by remember { mutableStateOf(emptyMap<String, List<PollOptionEntity>>()) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var replyTo by remember { mutableStateOf<MessageEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(context, conversationId) {
        val prefs = context.getSharedPreferences("ringtones", Context.MODE_PRIVATE)
        hasCustomRingtone = prefs.getString(conversationId, null) != null
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            coroutineScope.launch {
                val expireAt = if (ephemeralDuration != null) System.currentTimeMillis() + ephemeralDuration!! else null
                val msg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    content = "📷 Foto",
                    isOwn = true,
                    expireAt = expireAt,
                    mediaUri = cameraUri.toString(),
                    viewOnce = viewOnce
                )
                messages = messages + msg
                db?.messageDao()?.insertMessage(msg)
                cameraUri = null
            }
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val prefs = context.getSharedPreferences("ringtones", Context.MODE_PRIVATE)
            prefs.edit().putString(conversationId, it.toString()).apply()
            Toast.makeText(context, "Tono asignado", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (showBackgroundDialog) {
                backgroundImageUri = it
                showBackgroundDialog = false
            } else {
                pendingMediaUri = it
                editorText = ""
                showImageEditor = true
            }
        }
    }

    val status = when (conversationId) {
        "sim_alicia" -> "en línea"
        "sim_carlos" -> "últ. vez hoy"
        "sim_oficina" -> "en línea"
        else -> ""
    }

    LaunchedEffect(db, conversationId) {
        if (db != null) {
            val conv = db.conversationDao().getConversationById(conversationId)
            conversationBgColor = conv?.chatBackgroundColor
        }
    }

    LaunchedEffect(db, conversationId) {
        if (db != null) {
            db.pollDao().getPollsForGroup(conversationId).collect { pollList ->
                polls = pollList
                pollList.forEach { poll ->
                    db.pollDao().getOptionsForPoll(poll.id).collect { options ->
                        optionsMap = optionsMap + (poll.id to options)
                    }
                }
            }
        }
    }

    LaunchedEffect(db, conversationId) {
        if (db != null) {
            db.messageDao().getMessagesForConversation(conversationId).collect { list: List<MessageEntity> ->
                messages = list
            }
        } else {
            messages = sampleMessages[conversationId] ?: emptyList()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    BackHandler { onBack() }

    fun vibrateOnly() {
        coroutineScope.launch {
            repeat(5) {
                shakeOffset.animateTo(3f, animationSpec = tween(20))
                shakeOffset.animateTo(-3f, animationSpec = tween(20))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(15))
        }
        try {
            val mp = MediaPlayer.create(context, R.raw.zumbido)
            mp?.start()
        } catch (_: Exception) {}
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }

    fun startRecording() {
        val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.mp3")
        audioFile = file
        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar grabación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            if (audioFile != null) {
                coroutineScope.launch {
                    val msg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        content = "🎤 Nota de voz",
                        isOwn = true,
                        mediaUri = Uri.fromFile(audioFile).toString()
                    )
                    messages = messages + msg
                    db?.messageDao()?.insertMessage(msg)
                    NetworkService.sendMessage(
                        MeshMessage(content = msg.content, senderId = "self", timestamp = System.currentTimeMillis())
                    )
                }
            }
            Toast.makeText(context, "Nota de voz enviada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al detener grabación", Toast.LENGTH_SHORT).show()
        }
    }

    val groupedMessages = remember(messages, searchMessageQuery) {
        val filtered = if (searchMessageQuery.isBlank()) messages else messages.filter { it.content.contains(searchMessageQuery, ignoreCase = true) }
        filtered.groupBy { formatDateKey(it.timestamp) }
    }

    val bgColor = when {
        backgroundImageUri != null -> Color.Transparent
        conversationBgColor != null -> Color(conversationBgColor!!)
        gradientType == 1 -> Brush.horizontalGradient(listOf(Color(0xFFB0BEC5), Color(0xFF607D8B), Color(0xFF455A64)))
        gradientType == 2 -> Brush.horizontalGradient(listOf(Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFF39FF14)))
        gradientType == 3 -> Brush.horizontalGradient(listOf(Color(0xFF1A237E), Color(0xFF4A148C), Color(0xFF880E4F)))
        gradientType == 4 -> Brush.horizontalGradient(listOf(Color(0xFFFF6F00), Color(0xFFFF3D00), Color(0xFFDD2C00)))
        else -> when (chatBackground) {
            0 -> MaterialTheme.colorScheme.background
            1 -> Color(0xFF1A1A1A)
            2 -> Color(0xFF2B1A3D)
            3 -> Color(0xFF1A3D2B)
            4 -> Color(0xFF3D2B1A)
            5 -> Color(0xFFE0D7C6)
            6 -> Color(0xFFC6D7E0)
            7 -> Color(0xFFD7C6E0)
            8 -> Color(0xFFE0C6C6)
            9 -> Color(0xFFC6E0C6)
            else -> MaterialTheme.colorScheme.background
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra superior
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Volver")
                }
                Surface(
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(contactName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = contactName, style = MaterialTheme.typography.titleMedium)
                    if (isTyping) {
                        Text("escribiendo...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else if (status.isNotEmpty()) {
                        Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = "Cifrado E2E", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Cifrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        if (hasCustomRingtone) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.Notifications, contentDescription = "Tono personalizado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                val currentAvatar by IdentityManager.avatarBitmap.collectAsState()
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        if (currentAvatar != null) { Image(bitmap = currentAvatar!!.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.size(40.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), contentScale = ContentScale.Crop) } else { Surface(modifier = Modifier.size(40.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) { Box(contentAlignment = Alignment.Center) { Text(contactName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium) } } }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Ver perfil") }, onClick = { showMenu = false; Toast.makeText(context, "Ver perfil", Toast.LENGTH_SHORT).show() })
                        DropdownMenuItem(text = { Text("Buscar mensajes") }, onClick = { showMenu = false; Toast.makeText(context, "Buscar mensajes", Toast.LENGTH_SHORT).show() })
                        DropdownMenuItem(text = { Text("Cambiar fondo") }, onClick = { showMenu = false; showBackgroundDialog = true })
                        DropdownMenuItem(text = { Text("Silenciar notificaciones") }, onClick = { showMenu = false; Toast.makeText(context, "Silenciar", Toast.LENGTH_SHORT).show() })
                        DropdownMenuItem(text = { Text("Vaciar chat") }, onClick = { showMenu = false; Toast.makeText(context, "Vaciar chat", Toast.LENGTH_SHORT).show() })
                        DropdownMenuItem(text = { Text("Exportar chat") }, onClick = { showMenu = false; Toast.makeText(context, "Exportar chat", Toast.LENGTH_SHORT).show() })
                        DropdownMenuItem(text = { Text("Bloquear") }, onClick = { showMenu = false; Toast.makeText(context, "Bloquear", Toast.LENGTH_SHORT).show() })
                        DropdownMenuItem(text = { Text("Eliminar conversación") }, onClick = { showMenu = false; Toast.makeText(context, "Eliminar conversación", Toast.LENGTH_SHORT).show() })
                        DropdownMenuItem(text = { Text("Crear encuesta") }, onClick = { showMenu = false; showCreatePollDialog = true })
                        if (hasCustomRingtone) {
                            DropdownMenuItem(text = { Text("Probar tono") }, onClick = {
                                showMenu = false
                                val prefs = context.getSharedPreferences("ringtones", Context.MODE_PRIVATE)
                                val uriString = prefs.getString(conversationId, null)
                                if (uriString != null) {
                                    val uri = Uri.parse(uriString)
                                    val ringtone = RingtoneManager.getRingtone(context, uri)
                                    ringtone.play()
                                }
                            })
                        }
                        DropdownMenuItem(text = { Text("Asignar tono") }, onClick = { showMenu = false; ringtonePickerLauncher.launch("audio/*") })
                        DropdownMenuItem(text = { Text("Llamada de voz") }, onClick = { showMenu = false; onVoiceCallClick() })
                        DropdownMenuItem(text = { Text("Videollamada") }, onClick = { showMenu = false; onVideoCallClick() })
                        DropdownMenuItem(text = { Text("Exportar chat") }, onClick = { showMenu = false; exportChat(context, messages, contactName) })
                        DropdownMenuItem(text = { Text("Mensajes efímeros") }, onClick = { showMenu = false; showEphemeralMenu = true })
                        DropdownMenuItem(text = { Text("Modo fiesta") }, onClick = { showMenu = false; isFiestaMode = !isFiestaMode })
                    }
                }
            }
        }

        // Contenido del chat
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset(x = shakeOffset.value.dp)
        ) {
            if (backgroundImageUri != null) {
                AsyncImage(model = backgroundImageUri, contentDescription = "Fondo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else if (isFiestaMode) {
                FiestaBackground()
            } else if (gradientType > 0) {
                Box(modifier = Modifier.fillMaxSize().background(bgColor as Brush))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(bgColor as Color))
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (polls.isNotEmpty()) {
                    items(polls, key = { it.id }) { poll ->
                        val options = optionsMap[poll.id] ?: emptyList()
                        PollCard(poll = poll, options = options, onVote = { optionId ->
                            coroutineScope.launch {
                                db?.pollDao()?.incrementVoteCount(optionId, 1)
                                val updated = options.map { opt ->
                                    if (opt.id == optionId) opt.copy(voteCount = opt.voteCount + 1) else opt
                                }
                                optionsMap = optionsMap + (poll.id to updated)
                            }
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                groupedMessages.forEach { (dateKey, msgs) ->
                    item {
                        Text(
                            text = formatDateHeader(dateKey),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    items(msgs, key = { it.id }) { msg ->
                        val index = msgs.indexOf(msg)
                        val isGrouped = index > 0 && msgs[index - 1].isOwn == msg.isOwn
                        MessageBubble(
                            message = msg,
                            isGrouped = isGrouped,
                            onDelete = { id -> coroutineScope.launch { db?.messageDao()?.deleteMessage(id); messages = messages.filter { it.id != id } } },
                            onCopy = { text ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("mensaje", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Texto copiado", Toast.LENGTH_SHORT).show()
                            },
                            onForward = { msgToForward ->
                                Toast.makeText(context, "Reenviar (próximamente)", Toast.LENGTH_SHORT).show()
                            },
                            onReply = { msgToReply -> replyTo = msgToReply }
                        )
                    }
                }
            }
        }

        // Barra inferior con cita
        Column(modifier = Modifier.fillMaxWidth()) {
            // Barra de respuesta (si hay mensaje citado)
            if (replyTo != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Respondiendo a ${if (replyTo!!.isOwn) "ti" else contactName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = replyTo!!.content.take(100),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyTo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancelar respuesta", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Barra de escritura
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isMeshMode) {
                        IconButton(onClick = { vibrateOnly() }) {
                            Icon(Icons.Filled.Vibration, "Zumbido", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mensaje") },
                        maxLines = 1,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    if (!isMeshMode) {
                        IconButton(onClick = { showAttachmentSheet = true }) {
                            Icon(Icons.Filled.AttachFile, "Adjuntar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (text.isBlank()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            if (!isRecording) startRecording()
                                        },
                                        onTap = {
                                            if (isRecording) stopRecording()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRecording) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Stop, "Detener", tint = Color.Red)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(4) { i ->
                                            val infiniteTransition = rememberInfiniteTransition(label = "bar_$i")
                                            val height by infiniteTransition.animateFloat(
                                                initialValue = 8f,
                                                targetValue = 24f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(300, easing = LinearEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "bar_height_$i"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(height.dp)
                                                    .padding(horizontal = 2.dp)
                                                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                            )
                                        }
                                    }
                                }
                            } else {
                                Icon(Icons.Filled.Mic, "Grabar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val currentReply = replyTo
                                    val expireAt = if (ephemeralDuration != null) System.currentTimeMillis() + ephemeralDuration!! else null
                                    val msg = MessageEntity(
                                        id = UUID.randomUUID().toString(),
                                        conversationId = conversationId,
                                        content = text,
                                        isOwn = true,
                                        expireAt = expireAt,
                                        viewOnce = viewOnce,
                                        quotedMessageId = currentReply?.id,
                                        quotedMessageContent = currentReply?.content
                                    )
                                    messages = messages + msg
                                    db?.messageDao()?.insertMessage(msg)
                                    NetworkService.sendMessage(
                                        MeshMessage(
                                            content = text,
                                            senderId = "self",
                                            timestamp = System.currentTimeMillis(),
                                            quotedMessageId = currentReply?.id,
                                            quotedMessageContent = currentReply?.content
                                        )
                                    )
                                    text = ""
                                    replyTo = null
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Send, "Enviar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Selector multimedia (sin cambios, pero se mantiene)
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
                        IconButton(onClick = { showAttachmentSheet = false; filePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Filled.Photo, "Galería", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Galería", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = {
                            showAttachmentSheet = false
                            val tempFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        }) {
                            Icon(Icons.Filled.CameraAlt, "Cámara", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Cámara", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showAttachmentSheet = false; Toast.makeText(context, "Ubicación próximamente", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Filled.LocationOn, "Ubicación", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Ubicación", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showAttachmentSheet = false; Toast.makeText(context, "Contacto próximamente", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Filled.PersonAdd, "Contacto", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Contacto", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showAttachmentSheet = false; filePickerLauncher.launch("application/*") }) {
                            Icon(Icons.Filled.InsertDriveFile, "Documento", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Documento", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showAttachmentSheet = false; StickerState.openPicker() }) {
                            Icon(Icons.Filled.InsertEmoticon, "Sticker", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showAttachmentSheet = false; showCreatePollDialog = true }) {
                            Icon(Icons.Filled.Poll, "Encuesta", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Encuesta", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showAttachmentSheet = false; Toast.makeText(context, "Evento próximamente", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Filled.Event, "Evento", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Evento", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Editor de imagen (sin cambios)
    if (showImageEditor && pendingMediaUri != null) {
        AlertDialog(
            onDismissRequest = { showImageEditor = false },
            title = { Text("Editar imagen") },
            text = {
                Column {
                    AsyncImage(
                        model = pendingMediaUri,
                        contentDescription = "Imagen a enviar",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editorText,
                        onValueChange = { editorText = it },
                        label = { Text("Añade un texto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Autoeliminar:", modifier = Modifier.weight(1f))
                        listOf(5000L to "5s", 30000L to "30s", 60000L to "1min", null to "No").forEach { (dur, label) ->
                            FilterChip(
                                selected = ephemeralDuration == dur,
                                onClick = { ephemeralDuration = dur },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vista única", modifier = Modifier.weight(1f))
                        Switch(checked = viewOnce, onCheckedChange = { viewOnce = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val expireAt = if (ephemeralDuration != null) System.currentTimeMillis() + ephemeralDuration!! else null
                        val msg = MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            content = editorText.ifBlank { "📷 Imagen" },
                            isOwn = true,
                            expireAt = expireAt,
                            mediaUri = pendingMediaUri.toString(),
                            viewOnce = viewOnce
                        )
                        messages = messages + msg
                        db?.messageDao()?.insertMessage(msg)
                        NetworkService.sendMessage(
                            MeshMessage(content = msg.content, senderId = "self", timestamp = System.currentTimeMillis())
                        )
                        showImageEditor = false
                        pendingMediaUri = null
                    }
                }) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { showImageEditor = false }) { Text("Cancelar") } }
        )
    }

    // Diálogo de fondo (sin cambios)
    if (showBackgroundDialog) {
        var bgTab by remember { mutableStateOf(0) }
        AlertDialog(
            onDismissRequest = { showBackgroundDialog = false },
            title = { Text("Cambiar fondo") },
            text = {
                Column {
                    TabRow(selectedTabIndex = bgTab) {
                        Tab(selected = bgTab == 0, onClick = { bgTab = 0 }, text = { Text("Colores") })
                        Tab(selected = bgTab == 1, onClick = { bgTab = 1 }, text = { Text("Gradientes") })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (bgTab == 0) {
                        val allColors = listOf(
                            Color(0xFF0A1B2A), Color(0xFF1A1A1A), Color(0xFF2B1A3D), Color(0xFF1A3D2B), Color(0xFF3D2B1A),
                            Color(0xFFE0D7C6), Color(0xFFC6D7E0), Color(0xFFD7C6E0), Color(0xFFE0C6C6), Color(0xFFC6E0C6),
                            Color(0xFF4A148C), Color(0xFF0D47A1), Color(0xFF1B5E20), Color(0xFFBF360C), Color(0xFF37474F),
                            Color(0xFFF9A825), Color(0xFFAD1457), Color(0xFF006064), Color(0xFF5D4037), Color(0xFF827717)
                        )
                        val rows = 4; val cols = 5
                        for (r in 0 until rows) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (c in 0 until cols) {
                                    val index = r * cols + c
                                    if (index < allColors.size) {
                                        val color = allColors[index]
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(color, RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                                .then(if (index == chatBackground && backgroundImageUri == null && gradientType == 0) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                                                .clickable { chatBackground = index; gradientType = 0; backgroundImageUri = null; showBackgroundDialog = false }
                                        )
                                    } else { Spacer(modifier = Modifier.size(48.dp)) }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        val gradients = listOf(
                            "Metal" to listOf(Color(0xFFB0BEC5), Color(0xFF607D8B), Color(0xFF455A64)),
                            "Neón" to listOf(Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFF39FF14)),
                            "Galaxia" to listOf(Color(0xFF1A237E), Color(0xFF4A148C), Color(0xFF880E4F)),
                            "Atardecer" to listOf(Color(0xFFFF6F00), Color(0xFFFF3D00), Color(0xFFDD2C00))
                        )
                        gradients.forEachIndexed { index, (name, colors) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(Brush.horizontalGradient(colors), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { gradientType = index + 1; chatBackground = -1; backgroundImageUri = null; showBackgroundDialog = false }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(name, color = Color.White, style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showBackgroundDialog = false; filePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Image, null); Spacer(modifier = Modifier.width(8.dp)); Text("Elegir imagen")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        if (gradientType == 0 && chatBackground >= 0) {
                            val colorInt = when (chatBackground) {
                                0 -> 0xFF0A1B2A.toInt(); 1 -> 0xFF1A1A1A.toInt(); 2 -> 0xFF2B1A3D.toInt(); 3 -> 0xFF1A3D2B.toInt()
                                4 -> 0xFF3D2B1A.toInt(); 5 -> 0xFFE0D7C6.toInt(); 6 -> 0xFFC6D7E0.toInt(); 7 -> 0xFFD7C6E0.toInt()
                                8 -> 0xFFE0C6C6.toInt(); 9 -> 0xFFC6E0C6.toInt(); else -> null
                            }
                            if (colorInt != null) {
                                db?.conversationDao()?.updateChatBackgroundColor(conversationId, colorInt)
                                conversationBgColor = colorInt
                            }
                        }
                    }
                    showBackgroundDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showBackgroundDialog = false }) { Text("Cancelar") } }
        )
    }

    // Diálogo de efímeros (sin cambios)
    if (showEphemeralMenu) {
        AlertDialog(
            onDismissRequest = { showEphemeralMenu = false },
            title = { Text("Duración de los mensajes") },
            text = {
                Column {
                    listOf(5000L to "5 segundos", 30000L to "30 segundos", 60000L to "1 minuto", 300000L to "5 minutos", 3600000L to "1 hora").forEach { (dur, label) ->
                        TextButton(onClick = { ephemeralDuration = dur; showEphemeralMenu = false; Toast.makeText(context, "Mensajes efímeros: $label", Toast.LENGTH_SHORT).show() }) {
                            Text(if (ephemeralDuration == dur) "✓ $label" else label)
                        }
                    }
                    TextButton(onClick = { ephemeralDuration = null; showEphemeralMenu = false; Toast.makeText(context, "Mensajes normales", Toast.LENGTH_SHORT).show() }) {
                        Text(if (ephemeralDuration == null) "✓ Sin caducidad" else "Sin caducidad")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vista única", modifier = Modifier.weight(1f))
                        Switch(checked = viewOnce, onCheckedChange = { viewOnce = it })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEphemeralMenu = false }) { Text("Cerrar") } }
        )
    }

    // Diálogo de encuesta (sin cambios)
    if (showCreatePollDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePollDialog = false },
            title = { Text("Crear encuesta") },
            text = {
                Column {
                    OutlinedTextField(value = pollQuestion, onValueChange = { pollQuestion = it }, label = { Text("Pregunta") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Opciones:", style = MaterialTheme.typography.titleSmall)
                    pollOptions.forEachIndexed { index, option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = option, onValueChange = { newValue -> pollOptions = pollOptions.toMutableList().also { it[index] = newValue } },
                                label = { Text("Opción ${index + 1}") }, modifier = Modifier.weight(1f)
                            )
                            if (pollOptions.size > 2) IconButton(onClick = { pollOptions = pollOptions.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Filled.Remove, "Eliminar") }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (pollOptions.size < 5) TextButton(onClick = { pollOptions = pollOptions + "" }) { Text("+ Añadir opción") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val pollId = UUID.randomUUID().toString()
                        db?.pollDao()?.insertPoll(PollEntity(id = pollId, groupId = conversationId, question = pollQuestion, creatorId = "self"))
                        pollOptions.forEach { text -> if (text.isNotBlank()) db?.pollDao()?.insertOption(PollOptionEntity(id = UUID.randomUUID().toString(), pollId = pollId, text = text)) }
                        Toast.makeText(context, "Encuesta creada", Toast.LENGTH_SHORT).show()
                        showCreatePollDialog = false; pollQuestion = ""; pollOptions = listOf("", "")
                    }
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { showCreatePollDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isGrouped: Boolean,
    onDelete: (String) -> Unit = {},
    onCopy: (String) -> Unit = {},
    onForward: (MessageEntity) -> Unit = {},
    onReply: (MessageEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val ownBubble = AccessibilitySettings.ownBubbleColor.value
    val otherBubble = AccessibilitySettings.otherBubbleColor.value
    val color = if (message.isOwn) ownBubble ?: MaterialTheme.colorScheme.primaryContainer else otherBubble ?: MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isOwn) {
        if (ownBubble != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        if (otherBubble != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
    }
    val topPadding = if (isGrouped) 2.dp else 12.dp
    val currentStyle = AccessibilitySettings.bubbleStyle.value
    val shape = when (currentStyle) {
        BubbleStyle.MODERN -> if (isGrouped) {
            if (message.isOwn) BubbleShapes.ModernOwn else BubbleShapes.ModernOther
        } else BubbleShapes.ModernDefault
        BubbleStyle.ROUNDED -> BubbleShapes.Rounded
        BubbleStyle.COMIC -> if (message.isOwn) BubbleShapes.ComicOwn else BubbleShapes.ComicOther
        BubbleStyle.PIXEL -> BubbleShapes.Pixel
        BubbleStyle.COLA -> if (message.isOwn) BubbleShapes.ColaOwn else BubbleShapes.ColaOther
    }

    var showReactionPicker by remember { mutableStateOf(false) }
    var showMessageMenu by remember { mutableStateOf(false) }
    var currentReaction by remember { mutableStateOf(message.reaction) }
    val reactions = listOf("❤️", "👍", "😂", "😮", "😢", "😡")

    LaunchedEffect(message.expireAt, message.viewOnce) {
        if (message.viewOnce) {
            delay(5000)
            onDelete(message.id)
        } else if (message.expireAt != null) {
            val remaining = message.expireAt - System.currentTimeMillis()
            if (remaining > 0) {
                delay(remaining)
                onDelete(message.id)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .padding(horizontal = 12.dp),
        contentAlignment = if (message.isOwn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (showReactionPicker) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reactions.forEach { emoji ->
                    Text(
                        text = emoji, fontSize = 20.sp,
                        modifier = Modifier.clickable { currentReaction = if (currentReaction == emoji) null else emoji; showReactionPicker = false }
                    )
                }
            }
        }

        Surface(
            color = color, shape = shape, shadowElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.widthIn(max = 280.dp).pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { showReactionPicker = !showReactionPicker },
                    onLongPress = { showMessageMenu = true }
                )
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Mostrar mensaje citado si existe
                if (!message.quotedMessageContent.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (message.isOwn)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = message.quotedMessageContent!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                if (message.mediaUri != null) {
                    if (message.viewOnce && !message.isOwn) {
                        var revealed by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { revealed = true }
                        ) {
                            AsyncImage(
                                model = Uri.parse(message.mediaUri),
                                contentDescription = "Imagen de vista única",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .blur(if (revealed) 0.dp else 20.dp),
                                contentScale = ContentScale.Crop
                            )
                            if (!revealed) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.Visibility, "Tocar para ver", tint = Color.White, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Ver imagen (única)", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                        }
                        if (revealed) {
                            LaunchedEffect(Unit) {
                                delay(5000)
                                onDelete(message.id)
                            }
                        }
                    } else {
                        AsyncImage(
                            model = Uri.parse(message.mediaUri),
                            contentDescription = "Imagen adjunta",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f)
                    )
                    if (message.isOwn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val check = when (message.status) { 0 -> "✓"; 1 -> "✓✓"; 2 -> "✓✓"; else -> "" }
                        val checkColor = if (message.status == 2) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                        Text(text = check, style = MaterialTheme.typography.labelSmall, color = checkColor)
                    }
                    if (message.expireAt != null) {
                        var remainingSeconds by remember { mutableStateOf((message.expireAt - System.currentTimeMillis()) / 1000) }
                        LaunchedEffect(message.expireAt) {
                            while (remainingSeconds > 0) {
                                delay(1000)
                                remainingSeconds = (message.expireAt - System.currentTimeMillis()) / 1000
                            }
                        }
                        if (remainingSeconds > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${remainingSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                if (currentReaction != null) {
                    Text(
                        text = currentReaction!!, fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMessageMenu,
            onDismissRequest = { showMessageMenu = false }
        ) {
            DropdownMenuItem(text = { Text("Responder") }, onClick = {
                showMessageMenu = false
                onReply(message)
            })
            DropdownMenuItem(text = { Text("Copiar") }, onClick = {
                showMessageMenu = false
                onCopy(message.content)
            })
            DropdownMenuItem(text = { Text("Reenviar") }, onClick = {
                showMessageMenu = false
                onForward(message)
            })
            DropdownMenuItem(text = { Text("Eliminar") }, onClick = {
                showMessageMenu = false
                onDelete(message.id)
            })
            if (message.mediaUri != null) {
                DropdownMenuItem(text = { Text("Guardar en galería") }, onClick = {
                    showMessageMenu = false
                    Toast.makeText(context, "Guardar en galería (próximamente)", Toast.LENGTH_SHORT).show()
                })
                DropdownMenuItem(text = { Text("Compartir") }, onClick = {
                    showMessageMenu = false
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(message.mediaUri))
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir imagen"))
                })
            }
            Divider()
            DropdownMenuItem(text = { Text("Reacciones") }, onClick = {
                showMessageMenu = false
                showReactionPicker = true
            })
        }
    }
}

@Composable
fun PollCard(poll: PollEntity, options: List<PollOptionEntity>, onVote: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(poll.question, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            val totalVotes = options.sumOf { it.voteCount }
            options.forEach { option ->
                val percentage = if (totalVotes > 0) (option.voteCount.toFloat() / totalVotes * 100).toInt() else 0
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(option.text, modifier = Modifier.weight(1f))
                    Text("${option.voteCount} votos", style = MaterialTheme.typography.labelSmall)
                }
                LinearProgressIndicator(progress = { percentage / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (!poll.isClosed) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onVote(options.firstOrNull()?.id ?: "") }, modifier = Modifier.fillMaxWidth()) { Text("Votar (demo)") }
            }
        }
    }
}

@Composable
fun FiestaBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fiesta")
    val colors = listOf(Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFF39FF14), Color(0xFFFF6600), Color(0xFFBF00FF))
    val particles = remember { (1..12).map { Particle(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 30f + 10f, colors.random(), Random.nextFloat() * 0.5f + 0.1f, Random.nextFloat() * 0.5f + 0.1f) } }
    val animatedProgress by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(5000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "progress")
    Canvas(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
        particles.forEach { particle ->
            val x = (size.width * (particle.x + particle.speedX * animatedProgress)) % size.width
            val y = (size.height * (particle.y + particle.speedY * animatedProgress)) % size.height
            drawCircle(color = particle.color.copy(alpha = 0.4f), radius = particle.radius, center = Offset(x, y))
        }
    }
}

private data class Particle(val x: Float, val y: Float, val radius: Float, val color: Color, val speedX: Float, val speedY: Float)

private fun formatDateKey(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
}

private fun formatDateHeader(dateKey: String): String {
    val parts = dateKey.split("-")
    val cal = Calendar.getInstance().apply { set(Calendar.YEAR, parts[0].toInt()); set(Calendar.DAY_OF_YEAR, parts[1].toInt()) }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> "Hoy"
        cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) && cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) -> "Ayer"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(cal.timeInMillis))
    }
}

// Mapa de mensajes de muestra (simulación)
private fun exportChat(context: android.content.Context, messages: List<com.malla.mvp.data.entity.MessageEntity>, contactName: String) {
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val json = org.json.JSONObject()
        json.put("chat_name", contactName)
        json.put("exported_at", sdf.format(java.util.Date()))
        json.put("message_count", messages.size)
        val arr = org.json.JSONArray()
        messages.forEach { msg ->
            val obj = org.json.JSONObject()
            obj.put("sender", if (msg.isOwn) "Yo" else contactName)
            obj.put("content", msg.content)
            obj.put("timestamp", sdf.format(java.util.Date(msg.timestamp)))
            obj.put("is_own", msg.isOwn)
            obj.put("status", msg.status)
            arr.put(obj)
        }
        json.put("messages", arr)
        val file = java.io.File(context.cacheDir, "chat_${contactName}_${System.currentTimeMillis()}.json")
        file.writeText(json.toString(2))
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Exportar chat JSON"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error al exportar chat", android.widget.Toast.LENGTH_SHORT).show()
    }
}
private val sampleMessages = mapOf(
    "sim_alicia" to (0..20).map {
        MessageEntity(
            id = "sim_alicia_$it",
            conversationId = "sim_alicia",
            content = listOf("Hola", "¿Cómo estás?", "Bien, gracias", "¡Nos vemos!")[it % 4],
            timestamp = System.currentTimeMillis() - it * 60000,
            isOwn = it % 2 == 0
        )
    },
    "sim_carlos" to (0..15).map {
        MessageEntity(
            id = "sim_carlos_$it",
            conversationId = "sim_carlos",
            content = listOf("Buenos días", "¿Qué tal?", "Genial")[it % 3],
            timestamp = System.currentTimeMillis() - it * 120000,
            isOwn = it % 3 == 0
        )
    
    StickerPickerDialog()
    StickerFullScreenDialog()
}
)
