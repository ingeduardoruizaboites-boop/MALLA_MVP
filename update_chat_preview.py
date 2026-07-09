#!/usr/bin/env python3
import re

file_path = "app/src/main/java/com/malla/mvp/ui/screen/ChatScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# 1. Agregar estados nuevos después de pendingMediaUri y showImageEditor
old_states = 'var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }\n    var showImageEditor by remember { mutableStateOf(false) }'
new_states = old_states + '\n    val pendingMediaUris = remember { mutableStateListOf<Uri>() }\n    var captionText by remember { mutableStateOf("") }'
content = content.replace(old_states, new_states)

# 2. Insertar galleryLauncher después del filePickerLauncher (debajo de su bloque de cierre)
file_launcher_end = 'filePickerLauncher.launch(input)\n        }\n    }'
gallery_launcher_code = '''
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingMediaUris.addAll(uris)
            showImageEditor = false
        }
    }
'''
content = content.replace(file_launcher_end, file_launcher_end + gallery_launcher_code)

# 3. Cambiar en el ModalBottomSheet "Galería" para usar galleryLauncher
old_icon_button = 'IconButton(onClick = { showAttachmentSheet = false; filePickerLauncher.launch("image/*") })'
new_icon_button = 'IconButton(onClick = { showAttachmentSheet = false; galleryLauncher.launch("image/*") })'
content = content.replace(old_icon_button, new_icon_button)

# 4. Insertar barra de vista previa antes de "// Barra inferior con cita"
preview_bar = '''
        // Vista previa de imágenes múltiples (estilo WhatsApp)
        if (pendingMediaUris.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingMediaUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Miniatura",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            var showFull by remember { mutableStateOf(false) }
                                            if (showFull) {
                                                Dialog(
                                                    onDismissRequest = { showFull = false },
                                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.95f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        AsyncImage(
                                                            model = uri,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            modifier = Modifier.weight(2f),
                            placeholder = { Text("Añade un pie de foto...") },
                            maxLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall,
                            leadingIcon = {
                                IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                                    Icon(Icons.Filled.InsertEmoticon, "Emoji", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val uri = pendingMediaUris.firstOrNull()
                                    if (uri != null) {
                                        var showPreview by remember { mutableStateOf(false) }
                                        if (showPreview) {
                                            Dialog(
                                                onDismissRequest = { showPreview = false },
                                                properties = DialogProperties(usePlatformDefaultWidth = false)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.95f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AsyncImage(
                                                        model = uri,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Filled.Visibility, "Vista previa", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (captionText.isNotBlank()) {
                                    val txtMsg = MessageEntity(
                                        id = UUID.randomUUID().toString(),
                                        conversationId = conversationId,
                                        content = captionText,
                                        timestamp = System.currentTimeMillis(),
                                        isOwn = true
                                    )
                                    messages = messages + txtMsg
                                    db?.messageDao()?.insertMessage(txtMsg)
                                    NetworkService.sendMessage(
                                        MeshMessage(
                                            content = captionText,
                                            senderId = "self",
                                            timestamp = txtMsg.timestamp
                                        )
                                    )
                                }
                                pendingMediaUris.forEach { uri ->
                                    val imgMsg = MessageEntity(
                                        id = UUID.randomUUID().toString(),
                                        conversationId = conversationId,
                                        content = "",
                                        timestamp = System.currentTimeMillis(),
                                        isOwn = true,
                                        mediaUri = uri.toString()
                                    )
                                    messages = messages + imgMsg
                                    db?.messageDao()?.insertMessage(imgMsg)
                                    NetworkService.sendMessage(
                                        MeshMessage(
                                            content = uri.toString(),
                                            senderId = "self",
                                            timestamp = imgMsg.timestamp
                                        )
                                    )
                                }
                                pendingMediaUris.clear()
                                captionText = ""
                            }
                        }) {
                            Icon(Icons.Filled.Send, "Enviar imágenes", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(Icons.Filled.Add, "Añadir más", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            pendingMediaUris.clear()
                            captionText = ""
                        }) {
                            Icon(Icons.Filled.Close, "Cancelar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

'''

content = content.replace('// Barra inferior con cita', preview_bar + '\n        // Barra inferior con cita')

with open(file_path, "w") as f:
    f.write(content)

print("Archivo modificado exitosamente.")
