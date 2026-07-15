package com.malla.mvp.network

import android.content.Context
import com.malla.mvp.App
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.identity.IdentityManager
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object ContactDiscoveryManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun getMyPhone(): String {
        val prefs = App.context.getSharedPreferences("identity", Context.MODE_PRIVATE)
        return prefs.getString("user_phone", "") ?: ""
    }

    private fun getMyId(): String = IdentityManager.getIdentityId()

    // Salt compartido derivado determinísticamente del número de teléfono
    // usando PBKDF2 con muchas iteraciones para encarecer enumeración
    private fun getSharedSalt(phone: String): String {
        val salt = "malla-contact-salt-v2".toByteArray(Charsets.UTF_8)
        val spec = javax.crypto.spec.PBEKeySpec(
            phone.toCharArray(),
            salt,
            100000, // iteraciones PBKDF2
            256
        )
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }


    private fun hasLeadingZeroBits(hash: ByteArray, bits: Int): Boolean {
        var total = 0
        for (byte in hash) {
            if (byte == 0.toByte()) {
                total += 8
                if (total >= bits) return true
            } else {
                var mask = 0x80
                for (i in 0..7) {
                    if ((byte.toInt() and mask) == 0) {
                        total++
                        if (total >= bits) return true
                    } else {
                        return false
                    }
                    mask = mask shr 1
                }
            }
        }
        return total >= bits
    }

    private fun generateHashcash(challenge: String, difficultyBits: Int): Long {
        val startTime = System.currentTimeMillis()
        val timeoutMs = 5000L
        var nonce = 0L
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        while (true) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                LogBuffer.add("DISCOVERY", "Hashcash timeout: no se encontró nonce para $challenge en ${timeoutMs}ms")
                return -1L
            }
            val input = challenge + nonce.toString()
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            if (hasLeadingZeroBits(hash, difficultyBits)) {
                val elapsed = System.currentTimeMillis() - startTime
                LogBuffer.add("DISCOVERY", "Hashcash generado en ${elapsed}ms: nonce=$nonce")
                return nonce
            }
            nonce++
        }
    }

    fun testHashcashDifficulty(difficultyBits: Int): Long {
        val challenge = "test-${System.currentTimeMillis()}"
        val start = System.currentTimeMillis()
        val nonce = generateHashcash(challenge, difficultyBits)
        val end = System.currentTimeMillis()
        LogBuffer.add("DISCOVERY", "Tiempo para dificultad $difficultyBits: ${end - start}ms (nonce=$nonce)")
        return nonce
    }

    fun publishMyPresence() {
        scope.launch {
            val phone = getMyPhone()
            if (phone.isBlank()) return@launch
            val myId = getMyId()
            val todayHash = hashForDay(phone, 0)
            val yesterdayHash = hashForDay(phone, -1)
            val nonceToday = generateHashcash(todayHash, com.malla.mvp.network.DhtService.DIFFICULTY_BITS)
            val nonceYesterday = generateHashcash(yesterdayHash, com.malla.mvp.network.DhtService.DIFFICULTY_BITS)
            DhtService.publishDiscoveryHashcash(todayHash, nonceToday, myId)
            DhtService.publishDiscoveryHashcash(yesterdayHash, nonceYesterday, myId)
            LogBuffer.add("DISCOVERY", "Presencia publicada para $myId")
        }
    }

    fun searchByPhone(phoneNumber: String): String? {
        val todayHash = hashForDay(phoneNumber, 0)
        val yesterdayHash = hashForDay(phoneNumber, -1)
        return DhtService.findDiscovery(todayHash) ?: DhtService.findDiscovery(yesterdayHash)
    }

    private fun hashForDay(phone: String, dayOffset: Int): String {
        val day = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()) + dayOffset
        val input = phone + getSharedSalt(phone) + day.toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
