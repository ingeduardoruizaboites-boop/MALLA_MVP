═══ PROJECT_STATUS.md — Actualizado 2026-07-15 ═══
**Commit actual:** b1d2436b (main)
**Última compilación:** BUILD SUCCESSFUL con warnings no críticos.

## ── ESTADO GENERAL ──
App funcional: abre sin crash, navegación completa, chat persistente, notas de voz.
Fase actual: 3 – Comunicación Avanzada y Pre‑Mesh Discovery.
Avance estimado: ~80 %.

## ── PUNTOS RECIÉN CORREGIDOS (Ronda de estabilidad) ──
A. **Room/KSP** – Se agregó KSP en `:data` y el plugin en el raíz. Room compila correctamente.
B. **Identidad estable (TOFU/Ratchet)** – Se añadió `peerIdentity` en `ClientHandler` y `identityToClient` en `NetworkService` para mapeo estable.
C. **Broadcast UDP real para DHT** – `DhtService.broadcastMessage()` envía paquetes UDP a la red local. `ContactDiscoveryManager` lo usa para publicar presencia y buscar contactos.
D. **Caché de mensajes saltados** – `DoubleRatchet` ahora almacena claves futuras (`skippedKeys`) y las usa si falla el descifrado. Incluye `fillSkippedKeys` y `tryDecrypt`.
E. **Comentario TODO obsoleto eliminado** – Se removió de `Injector.kt`.

## ── DEUDA TÉCNICA PENDIENTE ──
- Pruebas reales de comunicación (BLE, Wi‑Fi Direct, DHT) entre dos dispositivos.
- Chat propio "Yo" (pendiente de implementación segura).
- Refactorización de Injector para romper dependencias circulares.
- Vista previa de imágenes estilo WhatsApp.
- Cobertura de pruebas unitarias.

## ── PRÓXIMOS PASOS ──
1. Implementar chat "Yo" de forma segura (como conversación normal en Room).
2. Probar comunicación real entre dispositivos.
3. Actualizar este archivo tras cada cambio significativo.
