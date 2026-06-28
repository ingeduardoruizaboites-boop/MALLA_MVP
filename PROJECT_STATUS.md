# MALLA MVP - Estado del Proyecto

**Última actualización:** 2026-06-28 (Sesión de rescate + Faro)
**Compilación:** BUILD SUCCESSFUL

---

## 🧾 Logros recientes

### Comunicación óptica (Faro)
- ✅ `LightEncoder`: protocolo binario con preámbulo, longitud, checksum.
- ✅ `FlashlightTransport`: transmisión con linterna, recepción con cámara (análisis de brillo).
- ✅ `FaroScreen`: UI con campo de texto, botones Transmitir / Recibir, barra de progreso.
- ✅ Nueva pestaña "Faro" en la barra de navegación inferior.

### Funcionalidades heredadas (presentes en el commit base)
- ✅ Servidor TCP (NetworkService) para comunicación LAN.
- ✅ Handshake ECDH y cifrado AES‑GCM.
- ✅ ChatScreen con burbujas personalizables y visor de imágenes.
- ✅ Pantallas: ConversationsScreen, PulsoScreen, PerfilScreen, SettingsScreen.
- ✅ Onboarding con identidad criptográfica.
- ✅ Escaneo BLE y Wi‑Fi Direct.

---

## 🚧 Pendientes (funcionalidades perdidas por reconstrucción)
- ❌ DHT distribuida y ContactDiscoveryManager
- ❌ PremiumManager y códecs mejorados
- ❌ WebRTC real con PeerConnection
- ❌ Stickers y visor a pantalla completa
- ❌ Ultrasonido real (FSK)
- ❌ SMS Transport
- ❌ Almacenamiento externo y vidrio esmerilado

---

## 🔄 Próximos pasos
1. Reconstruir `Injector.kt` completo con todos los transportes.
2. Recuperar DHT, Premium y WebRTC desde el historial de sesiones.
3. Integrar ultrasonido y SMS.
4. Pruebas con dos dispositivos cuando haya conectividad.

---

**Documento creado por:** DeepSeek (Arquitecto Principal)
**Sesión:** Rescate + Faro
**Fecha:** 2026-06-28
