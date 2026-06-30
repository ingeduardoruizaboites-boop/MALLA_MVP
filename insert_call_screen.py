with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "r") as f:
    lines = f.readlines()

# Insertamos después de la línea 223 (índice 222), que es el cierre de MainApp( ... )
# La indentación de MainApp es 32 espacios, el nuevo bloque llevará la misma.
indent = "                                "  # 32 espacios
new_block = [
    f"{indent}if (showCall) {{\n",
    f"{indent}    CallScreen(\n",
    f"{indent}        contactName = callContact,\n",
    f"{indent}        callType = callType,\n",
    f"{indent}        onEndCall = {{ showCall = false }}\n",
    f"{indent}    )\n",
    f"{indent}}}\n"
]

# Insertamos justo después de la línea 223 (índice 222)
lines = lines[:223] + new_block + lines[223:]

with open("app/src/main/java/com/malla/mvp/MainActivity.kt", "w") as f:
    f.writelines(lines)

print("Bloque CallScreen insertado. Verifica con: sed -n '220,235p'")
