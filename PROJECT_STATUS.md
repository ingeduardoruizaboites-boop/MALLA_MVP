═══ PROJECT_STATUS.md — Actualizado tras Ronda de Correcciones ═══
**Fecha:** 2026-07-14
**Commit actual:** 6dde5b63 (main)
**Última reconstrucción desde git:** 2026-07-13 (commit 5855d54b)

## ── ESTADO GENERAL DEL PROYECTO ──
Fase actual: 3 – Comunicación Avanzada y Pre‑Mesh Discovery
Porcentaje estimado de avance: ~80 %
Estado de compilación: COMPILA (BUILD SUCCESSFUL) con warnings no críticos.
App funcional: navegación, chat persistente, notas de voz push‑to‑talk, barra inferior modularizada, indicador de grabación, perfil encriptado, temas dinámicos.
Módulos huérfanos (:crypto, :network, :identity, :events, :transport) conectados a :app.

## ── ARQUITECTURA DE MÓDULOS ──
- :app → UI, ViewModels, Injector, Managers (dependencias con :network, :identity, etc.)
- :core → Interfaces, modelos, utilidades base, DoubleRatchet, KeystoreManager
- :data → Room, DAOs, repositorios, AppDatabase (con callback para chat propio)
- :crypto → CryptoEngine, adaptador
- :events → MallaEventBus (zumbidoReceived, messageReceived, etc.)
- :identity → IdentityManager (almacenamiento encriptado, claves, avatar)
- :transport → Transportes reales (BleTransport, WifiDirectTransport, TcpDirectTransport, MeshLinker)
- :media → VoiceRecorder, PttManager
- :network → BleManager, WifiDirectManager, ConnectivityMonitor, DhtService, MessageBridge, etc.

## ── CAMBIOS RECIENTES (desde último PROJECT_STATUS.md) ──

### Ronda de Correcciones de Seguridad (Puntos 1‑6)
1. **Double Ratchet real** – Implementado en `:core` con HKDF, chain keys, ratchet asimétrico. Integrado en `NetworkService.ClientHandler` (handshake, send, listenForMessages). Caché de mensajes saltados para tolerar desorden (hasta 20 claves). Persistencia del estado del ratchet en `SharedPreferences`. **Compila, pendiente prueba real con 2 dispositivos.**
2. **Handshake autenticado con TOFU** – `ClientHandler.start()` firma la clave efímera con ECDSA (KeystoreManager) y verifica la firma del peer. TOFU guarda identidad y rechaza cambios (posible MITM). **Compila, pendiente prueba real.**
3. **Salt de descubrimiento único por identidad** – Reemplazado el salt fijo por un salt determinístico derivado del número de teléfono con PBKDF2 (100k iteraciones). Así dos usuarios pueden calcular el mismo hash sin compartir un salt global. **Compila, pendiente prueba real.**
4. **Anti‑Sybil con hashcash** – `DhtService` exige hashcash (SHA‑256 con N=20 bits en cero). `ContactDiscoveryManager` genera el nonce. Timeout de 5s para evitar bucles infinitos. **Compila, pendiente prueba real.**
5. **Unicast en lugar de broadcast** – `NetworkService.sendMessageTo(recipientId, message)` envía solo al `ClientHandler` correcto, mapeando identidades a conexiones. Actualizadas todas las llamadas en `Injector`, `CascadeRouter`, `MeshChatViewModel`, etc. **Compila, pendiente prueba real.**
6. **Cascada real de transportes** – `CascadeRouter` ahora intenta BLE (`BleManager.connectAndReadIp`) y Wi‑Fi Direct (`WifiDirectManager.connectToPeer`) antes de guardar como pendiente. **Compila, pendiente prueba real.**

### Ronda de Integración (Puntos A‑G)
- **A:** Build limpio con `./gradlew clean assembleDebug` (BUILD SUCCESSFUL).
- **B:** DoubleRatchet conectado a NetworkService (reemplaza CryptoEngine estático). **Pendiente prueba real.**
- **C:** Descubrimiento de contactos arreglado: salt determinístico PBKDF2. **Pendiente prueba real.**
- **D:** Dificultad del hashcash corregida a bits reales (20) + timeout 5s. **Pendiente prueba real.**
- **E:** Verificación hashcash en handler UDP (`handleMessage`). **Pendiente prueba real.**
- **F:** `identityToClient` se actualiza siempre (no solo primer contacto) y se limpia en `disconnect()`. **Pendiente prueba real.**
- **G:** Nivel Internet de la cascada arreglado: `INetworkService.sendMeshMessage` recibe `recipientId` real desde `CascadeRouter`. **Pendiente prueba real.**

## ── DECISIONES TÉCNICAS RECIENTES ──
- Double Ratchet con caché de mensajes saltados para tolerar mensajes fuera de orden (típico en redes mesh).
- Salt de descubrimiento determinístico por número de teléfono (PBKDF2) en vez de salt fijo o por identidad.
- Hashcash con bits reales y timeout en vez de caracteres hex.
- Mapeo `identityToClient` en NetworkService para enrutar mensajes unicast por identidad, no por IP.
- Chat propio "Yo" intentado implementar mediante Room callback y modificaciones en UI, pero causó crash. **Pendiente de resolver de forma segura (deuda técnica).**

## ── ERRORES RESUELTOS EN ESTA SESIÓN ──
- Crash por `AppDatabase.getInstance()` nulo → manejo de null y fallback.
- Grabación de audio vacía → cambio a codec AAC con fuente MIC.
- `AnimatedVisibility` en Row → reemplazado por Crossfade.
- `Injector.messageRepo` no inicializado → verificación `isInitialized` en ChatViewModel.
- Indentación en `ConversationsScreen` que ocultaba lista → corrección de inserción del chat propio.
- Crash temprano por `WifiDirectManager` no inicializado → se añadió inicialización en Injector.
- Servicio `MeshChatService` no existente → eliminado del manifest.
- Duplicación de clases al conectar módulos huérfanos → eliminados 31 archivos duplicados.
- Handshake sin autenticación → añadida firma ECDSA + TOFU.
- Broadcast en TCP → corregido a unicast con `sendMessageTo`.
- Nivel Internet sin `recipientId` → corregido en toda la cadena (INetworkService, Injector, CascadeRouter, MessageBridge).

## ── DEUDA TÉCNICA ACTUAL ──
1. **Pruebas de comunicación real** – Todos los puntos de seguridad e integración requieren pruebas con 2 dispositivos (o más) para validar Double Ratchet, handshake autenticado, descubrimiento, hashcash, unicast, cascada BLE/Wi‑Fi Direct, etc.
2. **Chat propio "Yo"** – Funcionalidad no implementada de forma segura (causó crash en intentos anteriores). Se requiere un enfoque que no dependa de `IdentityManager` durante la composición.
3. **Refactorización de Injector** – Pendiente para eliminar dependencias circulares con `:network`.
4. **Vista previa de imágenes estilo WhatsApp** – Incompleta.
5. **Cobertura de pruebas unitarias** – Inexistente.
6. **Limpieza de warnings** – Múltiples deprecaciones y variables sin uso.

## ── PRÓXIMOS PASOS PRIORITARIOS ──
1. Realizar pruebas manuales de los puntos A‑G con dos dispositivos reales.
2. Implementar chat "Yo" de forma segura.
3. Refactorizar `Injector`.
4. Actualizar este archivo en cada commit significativo.

── RECONSTRUCCIÓN ──
Este archivo fue reconstruido el 2026-07-14 desde git log, cubriendo desde el commit 5855d54b hasta HEAD (6dde5b63), porque no se actualizó en tiempo real durante ese período. Cualquier decisión o contexto de esas sesiones que NO haya quedado reflejado en un commit se considera perdido y no está reflejado aquí.
