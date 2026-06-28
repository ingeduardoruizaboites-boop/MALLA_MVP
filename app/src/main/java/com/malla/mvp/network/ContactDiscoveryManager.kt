package com.malla.mvp.network

import com.malla.mvp.App
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.identity.IdentityManager
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object ContactDiscoveryManager {
    private const val SALT = "malla-contact-salt-v1"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun publishMyPresence() {
        scope.launch {
            val phone = IdentityManager.getUserPhone(App.context)
            if (phone.isBlank()) return@launch
            val myId = IdentityManager.getMyId()
            val todayHash = hashForDay(phone, 0)
            val yesterdayHash = hashForDay(phone, -1)
            DhtService.publishDiscovery(todayHash, myId)
            DhtService.publishDiscovery(yesterdayHash, myId)
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
        val input = phone + SALT + day.toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
