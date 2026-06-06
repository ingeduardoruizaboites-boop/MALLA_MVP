package com.malla.mvp.events

import com.malla.mvp.network.MeshMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bus de eventos centralizado para MALLA.
 * Único canal de comunicación entre módulos (transporte, crypto, UI).
 *
 * Referencia: Arquitectura V3.0 — Sección "Bus de Eventos"
 */
object MallaEventBus {

    // ── Conectividad ──────────────────────────────────────────────
    /** Nivel mesh activo (1-10) según DecisionEngine */
    val meshLevelChanged = MutableSharedFlow<Int>(replay = 1)
    /** Se restauró la conexión a internet */
    val internetRestored = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Lista de nodos cercanos detectados (direcciones/IPs) */
    val nearbyNodesUpdated = MutableSharedFlow<List<String>>(replay = 1)

    // ── Mensajería ────────────────────────────────────────────────
    /** Mensaje recibido desde la red mesh (ya validado y guardado) */
    val messageReceived = MutableSharedFlow<MeshMessage>(extraBufferCapacity = 10)
    /** Se detectó un intento de replay (mensaje duplicado) */
    val replayAttackDetected = MutableSharedFlow<String>(extraBufferCapacity = 5)

    // ── Hardware ──────────────────────────────────────────────────
    /** Nivel de batería actual (0-100) */
    val batteryLevelChanged = MutableStateFlow(100)
    /** Nuevo contacto agregado vía NFC (publicKey base64) */
    val nfcContactAdded = MutableSharedFlow<String>(extraBufferCapacity = 2)

    // ── UI ────────────────────────────────────────────────────────
    /** Solicitud de mostrar un Toast desde cualquier módulo */
    val showToast = MutableSharedFlow<String>(extraBufferCapacity = 3)
}
