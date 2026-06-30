package com.malla.mvp.network

import com.malla.mvp.core.engine.LogBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

object DhtService {
    const val DHT_PORT = 8887
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val socket = DatagramSocket(DHT_PORT)
    private val routingTable = ConcurrentHashMap<String, String>() // key -> "ip:port" o "discovery|id"
    private var running = false

    fun start() {
        running = true
        scope.launch {
            LogBuffer.add("DHT", "DHT iniciada en puerto $DHT_PORT")
            val buffer = ByteArray(1024)
            while (running) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    handleMessage(message, packet.address, packet.port)
                } catch (e: Exception) {
                    LogBuffer.add("DHT", "Error: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        running = false
        socket.close()
        scope.cancel()
    }

    fun publishDiscovery(hash: String, myId: String) {
        routingTable[hash] = "discovery|$myId"
        LogBuffer.add("DHT", "Publicado discovery: $hash")
    }

    fun findDiscovery(hash: String): String? {
        val entry = routingTable[hash] ?: return null
        if (entry.startsWith("discovery|")) return entry.removePrefix("discovery|")
        return null
    }

    fun publishMyAddress(myId: String, ip: String, port: Int) {
        routingTable[myId] = "$ip:$port"
        LogBuffer.add("DHT", "Dirección publicada: $myId -> $ip:$port")
    }

    fun lookupBlocking(id: String): String? = routingTable[id]

    private fun handleMessage(message: String, senderAddress: InetAddress, senderPort: Int) {
        val parts = message.split("|")
        when (parts[0]) {
            "PUBLISH" -> {
                if (parts.size == 4) {
                    routingTable[parts[1]] = "${parts[2]}:${parts[3]}"
                }
            }
            "FIND" -> {
                val entry = routingTable[parts.getOrElse(1) { return }]
                if (entry != null) {
                    val response = "FOUND|$entry"
                    val packet = DatagramPacket(response.toByteArray(), response.length, senderAddress, senderPort)
                    scope.launch { socket.send(packet) }
                }
            }
        }
    }
}
