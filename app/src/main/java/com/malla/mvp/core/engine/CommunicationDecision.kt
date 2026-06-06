package com.malla.mvp.core.engine

data class CommunicationDecision(
    val primaryTransport: MeshLevel = MeshLevel.BLE,
    val secondaryTransport: MeshLevel? = null,
    val scanIntervalSeconds: Int = 60,
    val maxPayloadBytes: Int = 256,
    val ttlHops: Int = 6,
    val useCompression: Boolean = false,
    val activateBloomFilter: Boolean = false,
    val reasoning: String = "Decisión por defecto segura"
) {
    companion object {
        fun defaultSafe() = CommunicationDecision(
            primaryTransport = MeshLevel.BLE,
            scanIntervalSeconds = 60,
            maxPayloadBytes = 256,
            ttlHops = 6,
            reasoning = "Decisión por defecto segura"
        )
    }
}
