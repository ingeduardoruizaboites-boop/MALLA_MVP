with open("app/src/main/java/com/malla/mvp/ui/screen/ChatScreen.kt", "r") as f:
    lines = f.readlines()

# 1. Añadir import de VoiceNoteRecorderDialog solo si no existe
if not any("VoiceNoteRecorderDialog" in line for line in lines):
    # Buscar la última línea de import y añadir después
    last_import_idx = max(i for i, line in enumerate(lines) if line.startswith("import "))
    lines.insert(last_import_idx + 1, "import com.malla.mvp.ui.components.VoiceNoteRecorderDialog\n")

# 2. Añadir estado showVoiceRecorder después de showAttachmentSheet
idx_attach = None
for i, line in enumerate(lines):
    if "var showAttachmentSheet by remember" in line:
        idx_attach = i
        break
if idx_attach is not None:
    lines.insert(idx_attach + 1, "    var showVoiceRecorder by remember { mutableStateOf(false) }\n")

# 3. Reemplazar TODO
for i, line in enumerate(lines):
    if "TODO: grabar audio" in line:
        lines[i] = line.replace("/* TODO: grabar audio */", "showVoiceRecorder = true")
        break

# 4. Insertar el diálogo antes de StickerPickerDialog()
idx_sticker = None
for i, line in enumerate(lines):
    if "StickerPickerDialog()" in line:
        idx_sticker = i
        break
if idx_sticker is not None:
    dialog_block = [
        "\n",
        "    if (showVoiceRecorder) {\n",
        "        VoiceNoteRecorderDialog(\n",
        "            onDismiss = { showVoiceRecorder = false },\n",
        "            onVoiceNoteReady = { uri ->\n",
        "                coroutineScope.launch {\n",
        "                    val msg = com.malla.mvp.data.entity.MessageEntity(\n",
        "                        id = java.util.UUID.randomUUID().toString(),\n",
        "                        conversationId = conversationId,\n",
        "                        text = \"\",\n",
        "                        timestamp = System.currentTimeMillis(),\n",
        "                        isOwn = true,\n",
        "                        mediaUri = uri.toString()\n",
        "                    )\n",
        "                    db?.messageDao()?.insertMessage(msg)\n",
        "                    com.malla.mvp.network.NetworkService.sendMessage(\n",
        "                        com.malla.mvp.network.MeshMessage(\n",
        "                            type = \"media\",\n",
        "                            conversationId = conversationId,\n",
        "                            content = uri.toString(),\n",
        "                            timestamp = msg.timestamp\n",
        "                        )\n",
        "                    )\n",
        "                }\n",
        "            }\n",
        "        )\n",
        "    }\n"
    ]
    for j, block_line in enumerate(dialog_block):
        lines.insert(idx_sticker + j, block_line)

with open("app/src/main/java/com/malla/mvp/ui/screen/ChatScreen.kt", "w") as f:
    f.writelines(lines)

print("Integración completada sin duplicados.")
