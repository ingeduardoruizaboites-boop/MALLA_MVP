with open("app/src/main/java/com/malla/mvp/ui/screen/ChatScreen.kt", "r") as f:
    lines = f.readlines()

# 1. Añadir import de VoiceNoteRecorderDialog (al inicio, línea 2 después de package)
lines.insert(1, "import com.malla.mvp.ui.components.VoiceNoteRecorderDialog\n")
lines.insert(1, "import android.net.Uri\n")
lines.insert(1, "import java.io.File\n")
lines.insert(1, "import com.malla.mvp.media.VoiceRecorder\n")

# 2. Añadir estados: buscar la línea donde se declara showAttachmentSheet (índice ~91)
idx_attach = None
for i, line in enumerate(lines):
    if "var showAttachmentSheet by remember" in line:
        idx_attach = i
        break
if idx_attach:
    lines.insert(idx_attach+1, "    var showVoiceRecorder by remember { mutableStateOf(false) }\n")

# 3. Modificar línea del TODO (la que contiene "TODO: grabar audio")
for i, line in enumerate(lines):
    if "TODO: grabar audio" in line:
        lines[i] = line.replace("/* TODO: grabar audio */", "showVoiceRecorder = true")
        break

# 4. Añadir invocación del diálogo justo antes de la línea que cierra el último if de showAttachmentSheet (después del bloque ModalBottomSheet)
# Buscamos "StickerPickerDialog()" o "StickerFullScreenDialog()" y añadimos antes
idx_sticker = None
for i, line in enumerate(lines):
    if "StickerPickerDialog()" in line:
        idx_sticker = i
        break
if idx_sticker:
    dialog_block = [
        "    if (showVoiceRecorder) {\n",
        "        VoiceNoteRecorderDialog(\n",
        "            onDismiss = { showVoiceRecorder = false },\n",
        "            onVoiceNoteReady = { uri ->\n",
        "                coroutineScope.launch {\n",
        "                    val msg = MessageEntity(\n",
        "                        id = UUID.randomUUID().toString(),\n",
        "                        conversationId = conversationId,\n",
        "                        text = \"\",\n",
        "                        timestamp = System.currentTimeMillis(),\n",
        "                        isOwn = true,\n",
        "                        mediaUri = uri.toString()\n",
        "                    )\n",
        "                    db?.messageDao()?.insertMessage(msg)\n",
        "                    NetworkService.sendMessage(\n",
        "                        MeshMessage(\n",
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

print("ChatScreen.kt modificado para notas de voz.")
