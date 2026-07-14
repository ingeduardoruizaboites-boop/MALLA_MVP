═══ PROJECT_STATUS.md — Reconstruido desde git ═══
**Fecha:** 2026-07-13
**Commit actual:** c047b788 (HEAD -> main)
**Última actualización manual:** commit 5855d54b (2026-06-28)

## ── ESTADO GENERAL DEL PROYECTO ──
Fase actual: 3 – Comunicación Avanzada y Pre‑Mesh Discovery
Porcentaje estimado de avance: ~75 %
Estado de compilación: COMPILA (BUILD SUCCESSFUL) con warnings.
App funcional: navegación, chat persistente, notas de voz push-to-talk, barra inferior modularizada, indicador de grabación, perfil encriptado, temas dinámicos.

## ── ARQUITECTURA DE MÓDULOS ──
- :app → UI, ViewModels, Injector, Managers (dependencias circulares con :network por resolver)
- :core → Interfaces, modelos, utilidades base
- :data → Room, DAOs, repositorios
- :crypto → CryptoEngine, adaptador
- :events → MallaEventBus
- :identity → IdentityManager (almacenamiento encriptado)
- :transport → Transportes reales (BleTransport, WifiDirectTransport, TcpDirectTransport, MeshLinker)
- :media → VoiceRecorder, PttManager
- :network → BleManager, WifiDirectManager, ConnectivityMonitor, DhtService, MessageBridge, etc.

## ── RESUMEN DE CAMBIOS DESDE ÚLTIMO PROJECT_STATUS.md ──
(agrupados por funcionalidad, evidencia en git log)

### Modularización completa (commits dd7cc1fc, 2a8d936b)
- Se crearon los módulos :core, :data, :crypto, :events, :identity, :media, :network, :transport.
- Se movieron entidades Room, DAOs, interfaces y managers a sus módulos.
- `Injector` se adaptó para usar los nuevos módulos.
- Se eliminaron archivos duplicados (DoubleRatchet, IdenticonGenerator, PulseManager, transportes viejos, pantallas obsoletas).

### Reconstrucción de servicios de red y comunicaciones (f7df3d36..14caf107)
- `Injector` completamente reconstruido con FlashlightTransport, NetworkService, MessageBridge, SmsTransport, UnifiedMessageRouter.
- `MessageBridge` con callbacks de envío/recepción, forward y procesamiento unificado.
- `DhtService`, `SeedManager`, `ContactDiscoveryManager` integrados.
- `PulsoScreen` restaurado desde backup; switch SMS en pestaña MODOS.
- `FaroScreen` reactivado con comunicación óptica.

### ChatScreen y UI premium (20179170..95b8fc5b)
- ChatScreen funcional con burbujas premium, shake global, envío de imágenes (GalleryPickerPanel), zoom, zumbido, barra de texto animada.
- `ChatViewModel` con persistencia en Room.
- `CascadeRouter` y `MessageReceiver` para encaminamiento de mensajes.
- `MainTopBar` con avatar 44dp, borde cian, indicador de conexión inteligente.
- `PerfilScreen` con almacenamiento encriptado de identidad (IdentityManager).
- `Stickers` integrados con selector y visor a pantalla completa.
- Navegación corregida (BackHandler en submenús, doble toque para salir).
- Control automático de Bluetooth/WiFi con `RadioManager` y servicio foreground.

### Notas de voz push-to-talk (e601265d..cbd22ef8)
- `VoiceRecorder` mejorado (AAC) con `amplitude` StateFlow.
- `VoiceRecorderButton` modularizado con animación de onda sinusoidal.
- `ChatInputBar` integrado con campo de texto, zumbido y botón de micrófono.
- Indicador de grabación estilo ondas en barra inferior.
- Auto-scroll al último mensaje implementado.

### Estabilización y permisos (ebfb098e, 2803c05b, 50da606e, fe8efead)
- Protección del servicio foreground con permiso POST_NOTIFICATIONS.
- Permisos solicitados al iniciar (ubicación, Bluetooth, cámara, etc.).
- Restauración de estados de Bluetooth/WiFi en onDestroy.

## ── DECISIONES TÉCNICAS IDENTIFICADAS ──
1. No refactorizar `Injector` para romper ciclos con :network en esta fase.
2. Uso de AAC para grabación de voz (calidad y compatibilidad).
3. Extracción de la barra de chat a `ChatInputBar` independiente.
4. Indicador de grabación visual con onda sinusoidal animada.
5. Chat propio "Yo" (self_chat) añadido en ConversationsScreen.
6. Uso de `EncryptedSharedPreferences` para datos sensibles de identidad.
7. Eliminación de PulseManager y lógica de decisión automática; se delegó a ConnectivityMonitor y selección manual en PulsoScreen.

## ── ERRORES RESUELTOS (según commits) ──
- Crash por `AppDatabase.getInstance()` nulo → manejo de null y fallback.
- Grabación de audio vacía → cambio a codec AAC con fuente MIC.
- `AnimatedVisibility` en Row → reemplazado por Crossfade.
- `Injector.messageRepo` no inicializado → verificación `isInitialized` en ChatViewModel.
- Indentación en `ConversationsScreen` que ocultaba lista → corrección de inserción del chat propio.
- Crash temprano por `WifiDirectManager` no inicializado → se añadió inicialización en Injector (sesión actual).
- Duplicación de chat "Yo" en ConversationsScreen → eliminado.

## ── DEUDA TÉCNICA ACTUAL ──
1. **Pruebas de comunicación real** – BLE, Wi‑Fi Direct, TCP no se han probado entre dispositivos físicos.
2. **Refactorización de Injector** – pendiente para eliminar dependencias circulares con :network.
3. **Vista previa de imágenes estilo WhatsApp** – incompleta, se requiere `GetContent` y lista `pendingMediaUris`.
4. **Notas de voz** – el indicador visual de ondas a veces no se mueve (posible problema de permisos o amplitud cero).
5. **Icono de notificación grande** (`setLargeIcon`) no implementado.
6. **Cobertura de pruebas unitarias** – inexistente.
7. **Limpieza de warnings** – hay múltiples deprecaciones y variables sin uso.

## ── PRÓXIMOS PASOS PRIORITARIOS ──
1. Verificar que el chat "Yo" funciona correctamente.
2. Probar grabación y reproducción de notas de voz con permisos adecuados.
3. Implementar vista previa de imágenes adjuntas.
4. Pruebas de comunicación en dispositivos reales.
5. Refactorizar `Injector`.
6. Actualizar este archivo en cada commit significativo.

── RECONSTRUCCIÓN ──
Este archivo fue reconstruido el 2026-07-13 desde git log, cubriendo desde el commit 5855d54b hasta HEAD (c047b788), porque no se actualizó en tiempo real durante ese período. Cualquier decisión o contexto de esas sesiones que NO haya quedado reflejado en un commit (conversaciones, descartes, razones no documentadas en el mensaje del commit) se considera perdido y no está reflejado aquí.
