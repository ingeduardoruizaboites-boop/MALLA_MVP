package com.malla.mvp.network

import kotlinx.coroutines.*
import java.util.BitSet

/**
 * Filtro Bloom de 64 KB (524,288 bits) para deduplicación de mensajes mesh.
 * Reduce retransmisiones duplicadas en ~99.7%.
 *
 * Funciones hash: FNV-1a, Murmur3 (simplificado), DJB2.
 * Rotación automática cada 7200 segundos (2 horas).
 *
 * Referencia: Arquitectura V3.0 — Sección "Bloom Filter"
 */
class BloomFilter(private val sizeBytes: Int = 65536) {

    private val bitSet = BitSet(sizeBytes * 8)  // 524,288 bits
    private var rotationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Añade un messageId al filtro */
    fun add(messageId: String) {
        val h1 = fnv1a(messageId)
        val h2 = murmur3(messageId)
        val h3 = djb2(messageId)
        bitSet.set(h1)
        bitSet.set(h2)
        bitSet.set(h3)
    }

    /** Verifica si un messageId podría estar en el filtro */
    fun mightContain(messageId: String): Boolean {
        val h1 = fnv1a(messageId)
        val h2 = murmur3(messageId)
        val h3 = djb2(messageId)
        return bitSet.get(h1) && bitSet.get(h2) && bitSet.get(h3)
    }

    /** Reinicia el filtro (llamado automáticamente cada 7200s) */
    fun reset() {
        bitSet.clear()
    }

    /** Inicia la rotación automática cada 7200 segundos */
    fun startAutoRotation() {
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (isActive) {
                delay(7_200_000L)  // 7200 segundos
                reset()
            }
        }
    }

    /** Detiene la rotación automática */
    fun stopAutoRotation() {
        rotationJob?.cancel()
        rotationJob = null
    }

    // ── Funciones hash ────────────────────────────────────────────

    private fun bitIndex(hash: Int): Int {
        return (hash and 0x7FFFFFFF) % (sizeBytes * 8)
    }

    private fun fnv1a(input: String): Int {
        var hash = -2128831035  // 0x811C9DC5 como Int
        for (b in input.toByteArray()) {
            hash = hash xor (b.toInt() and 0xFF)
            hash *= 16777619
        }
        return bitIndex(hash)
    }

    private fun murmur3(input: String): Int {
        val data = input.toByteArray()
        val c1 = -862048943  // 0xCC9E2D51
        val c2 = 461845907   // 0x1B873593
        var h1 = 0
        val length = data.size
        val nBlocks = length / 4
        for (i in 0 until nBlocks) {
            var k1 = (data[i * 4].toInt() and 0xFF) or
                    ((data[i * 4 + 1].toInt() and 0xFF) shl 8) or
                    ((data[i * 4 + 2].toInt() and 0xFF) shl 16) or
                    ((data[i * 4 + 3].toInt() and 0xFF) shl 24)
            k1 *= c1
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= c2
            h1 = h1 xor k1
            h1 = Integer.rotateLeft(h1, 13)
            h1 = h1 * 5 + -430675100  // 0xE6546B64
        }
        var k1 = 0
        val tail = length - nBlocks * 4
        if (tail >= 1) k1 = k1 or (data[nBlocks * 4].toInt() and 0xFF)
        if (tail >= 2) k1 = k1 or ((data[nBlocks * 4 + 1].toInt() and 0xFF) shl 8)
        if (tail >= 3) k1 = k1 or ((data[nBlocks * 4 + 2].toInt() and 0xFF) shl 16)
        if (tail >= 4) k1 = k1 or ((data[nBlocks * 4 + 3].toInt() and 0xFF) shl 24)
        if (tail > 0) {
            k1 *= c1
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= c2
            h1 = h1 xor k1
        }
        h1 = h1 xor length
        h1 = h1 xor (h1 ushr 16)
        h1 *= -2048144789  // 0x85EBCA77
        h1 = h1 xor (h1 ushr 13)
        h1 *= -1028477387  // 0xC2B2AE35
        h1 = h1 xor (h1 ushr 16)
        return bitIndex(h1)
    }

    private fun djb2(input: String): Int {
        var hash = 5381
        for (c in input) {
            hash = ((hash shl 5) + hash) + c.code  // hash * 33 + c
        }
        return bitIndex(hash)
    }
}
