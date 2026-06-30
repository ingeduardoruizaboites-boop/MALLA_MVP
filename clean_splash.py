import re

with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "r") as f:
    lines = f.readlines()

# Eliminamos el bloque huérfano desde la línea 289 ("@Composable" sin función) hasta la línea 319 ("}")
# Ajustamos índices base 1 a base 0: línea 289 -> índice 288, línea 319 -> índice 318
# Dejamos una línea en blanco para mantener la separación entre SettingsScreenWrapper y MainApp
new_lines = lines[:288] + ["\n"] + lines[319:]

with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "w") as f:
    f.writelines(new_lines)

print("Bloque sobrante eliminado. Verifica líneas alrededor con: sed -n '285,330p'")
