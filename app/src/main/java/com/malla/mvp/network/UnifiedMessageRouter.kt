package com.malla.mvp.network

import android.content.Context
import com.malla.mvp.App
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.di.Injector
import kotlinx.coroutines.*

object UnifiedMessageRouter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastSeen = mutableMapOf<String, MutableMap<String, Long>>()

    fun recordAvailability(contactId: String, transport: String) {
        val map = lastSeen.getOrPut(contactId) { mutableMapOf() }
        map[transport] = System.currentTimeMillis()
    }

    fun sendMessage(contactId: String, content: String) {
        scope.launch {
            // 1. Intentar TCP (Internet)
            if (isTransportAvailable(contactId, "tcp")) {
                try {
                    Injector.networkService.sendMeshMessage(
                        com.malla.mvp.core.network.MeshMessage(senderId = "Yo", content = content, type = 0)
                    )
                    LogBuffer.add("ROUTER", "Enviado a $contactId por TCP")
                    return@launch
                } catch (e: Exception) {
                    LogBuffer.add("ROUTER", "TCP falló: ${e.message}")
                }
            }

            // 2. Intentar SMS si está activado
            val prefs = App.context.getSharedPreferences("malla_prefs", Context.MODE_PRIVATE)
            val smsEnabled = prefs.getBoolean("sms_fallback", false)
            if (smsEnabled) {
                val phone = getContactPhone(contactId)
                if (phone.isNotBlank()) {
                    Injector.smsTransport.sendSms(phone, content)
                    LogBuffer.add("ROUTER", "Enviado a $contactId por SMS")
                    return@launch
                }
            }

            // 3. Guardar como pendiente
            LogBuffer.add("ROUTER", "Mensaje pendiente para $contactId")
        }
    }

    private fun isTransportAvailable(contactId: String, transport: String): Boolean {
        val map = lastSeen[contactId] ?: return false
        val last = map[transport] ?: return false
        return System.currentTimeMillis() - last < 300_000 // 5 minutos
    }

    private fun getContactPhone(contactId: String): String {
        // Simplificado: buscar en preferencias o contactos
        return ""
    }
}
