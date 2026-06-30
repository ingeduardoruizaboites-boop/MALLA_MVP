import re

# 1. Modificar ChatScreen.kt
with open("app/src/main/java/com/malla/mvp/ui/screen/ChatScreen.kt", "r") as f:
    chat_lines = f.readlines()

# Línea 82: cambiar firmas
chat_lines[81] = chat_lines[81].replace(
    "onVoiceCallClick: () -> Unit = {}, onVideoCallClick: () -> Unit = {}",
    "onVoiceCallClick: (String) -> Unit = {}, onVideoCallClick: (String) -> Unit = {}"
)
# Línea 407: invocación de Llamada de voz
chat_lines[406] = chat_lines[406].replace(
    "onVoiceCallClick()",
    "onVoiceCallClick(contactName)"
)
# Línea 408: invocación de Videollamada
chat_lines[407] = chat_lines[407].replace(
    "onVideoCallClick()",
    "onVideoCallClick(contactName)"
)

with open("app/src/main/java/com/malla/mvp/ui/screen/ChatScreen.kt", "w") as f:
    f.writelines(chat_lines)

# 2. Modificar MainActivity.kt
with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "r") as f:
    main_lines = f.readlines()

# Línea 220: onVoiceCallClick con lambda que recibe name
main_lines[219] = main_lines[219].replace(
    "{ showCall = true; callContact = \"Contacto\"; callType = \"voice\" }",
    "{ name -> showCall = true; callContact = name; callType = \"voice\" }"
)
# Línea 221: onVideoCallClick con lambda que recibe name
main_lines[220] = main_lines[220].replace(
    "{ showCall = true; callContact = \"Contacto\"; callType = \"video\" }",
    "{ name -> showCall = true; callContact = name; callType = \"video\" }"
)

# Línea 294: firma de MainApp onVoiceCallClick
main_lines[293] = main_lines[293].replace(
    "onVoiceCallClick: () -> Unit = {}",
    "onVoiceCallClick: (String) -> Unit = {}"
)
# Línea 295: firma de MainApp onVideoCallClick
main_lines[294] = main_lines[294].replace(
    "onVideoCallClick: () -> Unit = {}",
    "onVideoCallClick: (String) -> Unit = {}"
)

with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "w") as f:
    f.writelines(main_lines)

print("Cambios aplicados en ChatScreen.kt y MainActivity.kt")
