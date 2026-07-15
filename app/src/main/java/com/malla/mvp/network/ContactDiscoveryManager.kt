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

    private fun getPersonalSalt(): String {
        val pubKeyB64 = IdentityManager.getPublicKeyBase64() ?: return "malla-contact-salt-v1" // fallback
        val pubKeyBytes = android.util.Base64.decode(pubKeyB64, android.util.Base64.NO_WRAP)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pubKeyBytes)
        return hash.joinToString("") { "%02x".format(it) }
    }


    private fun generateHashcash(challenge: String, difficulty: Int): Long {
        val requiredPrefix = "0".repeat(difficulty)
        var nonce = 0L
        val startTime = System.currentTimeMillis()
        while (true) {
            val input = challenge + nonce.toString()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            val hexHash = hash.joinToString("") { "%02x".format(it) }
            if (hexHash.startsWith(requiredPrefix)) {
                val elapsed = System.currentTimeMillis() - startTime
                LogBuffer.add("DISCOVERY", "Hashcash generado en ${elapsed}ms: nonce=$nonce")
                return nonce
            }
            nonce++
        }
    }

    fun testHashcashDifficulty(difficulty: Int): Long {
        val challenge = "test-${System.currentTimeMillis()}"
        val start = System.currentTimeMillis()
        val nonce = generateHashcash(challenge, difficulty)
        val end = System.currentTimeMillis()
        LogBuffer.add("DISCOVERY", "Tiempo para dificultad $difficulty: ${end - start}ms (nonce=$nonce)")
        return nonce
    }

    fun publishMyPresence() {
        scope.launch {
            val phone = getMyPhone()
            if (phone.isBlank()) return@launch
            val myId = getMyId()
            val todayHash = hashForDay(phone, 0)
            val yesterdayHash = hashForDay(phone, -1)
            val nonceToday = generateHashcash(todayHash, com.malla.mvp.network.DhtService.DIFFICULTY)
            val nonceYesterday = generateHashcash(yesterdayHash, com.malla.mvp.network.DhtService.DIFFICULTY)
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
        val input = phone + getPersonalSalt() + day.toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
