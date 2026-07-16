package com.malla.mvp.network

import android.content.Context
import com.malla.mvp.App
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.network.NetworkService
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object ContactDiscoveryManager {
    private const val SALT = "malla-contact-salt-v1"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun getMyPhone(): String {
        val prefs = App.context.getSharedPreferences("identity", Context.MODE_PRIVATE)
        return prefs.getString("user_phone", "") ?: ""
    }

    private fun getMyId(): String = IdentityManager.getIdentityId()

    fun publishMyPresence() {
        scope.launch {
            val phone = getMyPhone()
            if (phone.isBlank()) return@launch
            val myId = getMyId()
            val myIp = NetworkService.getLocalIpAddress()
            val todayHash = hashForDay(phone, 0)
            val yesterdayHash = hashForDay(phone, -1)
            DhtService.publishDiscovery(todayHash, myId)
            DhtService.publishDiscovery(yesterdayHash, myId)
            DhtService.broadcastMessage("PUBLISH|$todayHash|$myIp|${DhtService.DHT_PORT}")
            DhtService.broadcastMessage("PUBLISH|$yesterdayHash|$myIp|${DhtService.DHT_PORT}")
            LogBuffer.add("DISCOVERY", "Broadcast de presencia enviado para $myId desde IP $myIp")
        }
    }

    fun searchByPhone(phoneNumber: String): String? {
        val todayHash = hashForDay(phoneNumber, 0)
        val yesterdayHash = hashForDay(phoneNumber, -1)
        DhtService.findDiscovery(todayHash)?.let { return it }
        DhtService.findDiscovery(yesterdayHash)?.let { return it }
        DhtService.broadcastMessage("FIND|$todayHash")
        DhtService.broadcastMessage("FIND|$yesterdayHash")
        LogBuffer.add("DISCOVERY", "Broadcast FIND enviado para $phoneNumber")
        Thread.sleep(200)
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
