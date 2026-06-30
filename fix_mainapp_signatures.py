with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "r") as f:
    lines = f.readlines()

# Línea 301 (índice 300) y 302 (índice 301): cambiar firmas
lines[300] = "    onVoiceCallClick: (String) -> Unit = {},\n"
lines[301] = "    onVideoCallClick: (String) -> Unit = {},\n"

with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "w") as f:
    f.writelines(lines)

print("Firmas de MainApp corregidas.")
