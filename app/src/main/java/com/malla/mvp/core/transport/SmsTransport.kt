package com.malla.mvp.core.transport

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import com.malla.mvp.core.engine.LogBuffer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class SmsTransport(private val context: Context) {
    private val _incomingMessages = MutableSharedFlow<String>(replay = 0)
    val incomingMessages: SharedFlow<String> = _incomingMessages

    private var receiver: BroadcastReceiver? = null
    private var registered = false

    fun start() {
        if (registered) return
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages?.forEach { sms ->
                    val body = sms.messageBody
                    val sender = sms.originatingAddress ?: "unknown"
                    LogBuffer.add("SMS", "Recibido de $sender: $body")
                    _incomingMessages.tryEmit("$sender|$body")
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
        registered = true
        LogBuffer.add("SMS", "Transporte SMS iniciado")
    }

    fun stop() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        registered = false
        LogBuffer.add("SMS", "Transporte SMS detenido")
    }

    fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            LogBuffer.add("SMS", "Enviado a $phoneNumber: ${message.take(50)}")
        } catch (e: Exception) {
            LogBuffer.add("SMS", "Error enviando SMS: ${e.message}")
        }
    }
}
