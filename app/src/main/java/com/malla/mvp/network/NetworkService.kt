package com.malla.mvp.network

import com.malla.mvp.crypto.CryptoEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*
import java.security.*
import javax.crypto.SecretKey
import android.util.Log

object NetworkService {
    private const val TAG = "NetworkService"
    const val DEFAULT_PORT = 8888

    private val _messages = MutableSharedFlow<MeshMessage>(replay = 10)
    val messages: SharedFlow<MeshMessage> = _messages.asSharedFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val serverJob = Job()
    private val serverScope = CoroutineScope(Dispatchers.IO + serverJob)
    private val clients = mutableMapOf<String, ClientHandler>()
    private val localKeyPair = CryptoEngine.generateKeyPair()
    val localPublicKeyBase64 = CryptoEngine.publicKeyToBase64(localKeyPair.public)

    fun startServer() {
        serverScope.launch {
            try {
                val serverSocket = ServerSocket(DEFAULT_PORT)
                Log.d(TAG, "Servidor iniciado en puerto $DEFAULT_PORT")
                while (isActive) {
                    val clientSocket = serverSocket.accept()
                    val handler = ClientHandler(clientSocket)
                    clients[handler.clientId] = handler
                    handler.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en servidor: ${e.message}")
            }
        }
    }

    fun stopServer() {
        serverJob.cancel()
        clients.values.forEach { it.disconnect() }
        clients.clear()
        _connectedClientsCount.value = 0
    }

    fun connectToPeer(address: String) {
        serverScope.launch {
            try {
                val socket = Socket(address, DEFAULT_PORT)
                val handler = ClientHandler(socket)
                clients[handler.clientId] = handler
                handler.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error conectando a $address: ${e.message}")
            }
        }
    }

    fun sendMessage(message: MeshMessage) {
        serverScope.launch {
            clients.values.forEach { it.send(message) }
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(":") == false) {
                        return address.hostAddress ?: "Desconocida"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo IP", e)
        }
        return "Desconocida"
    }

    // Clase sin 'inner' (no puede ser inner en un object)
    class ClientHandler(private val socket: Socket) {
        val clientId = "${socket.inetAddress.hostAddress}:${socket.port}"
        private var input: DataInputStream? = null
        private var output: DataOutputStream? = null
        private var secretKey: SecretKey? = null
        private var running = false

        fun start() {
            running = true
            try {
                input = DataInputStream(socket.getInputStream())
                output = DataOutputStream(socket.getOutputStream())

                output?.writeUTF(localPublicKeyBase64)
                output?.flush()
                val peerPubKeyBase64 = input?.readUTF() ?: throw Exception("No se recibió clave pública")
                val peerPublicKey = CryptoEngine.base64ToPublicKey(peerPubKeyBase64)
                secretKey = CryptoEngine.deriveSharedSecret(localKeyPair.private, peerPublicKey)
                Log.d(TAG, "Handshake completado con $clientId")
                _connectedClientsCount.value = clients.size

                serverScope.launch {
                    listenForMessages()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Handshake fallido: ${e.message}")
                disconnect()
            }
        }

        private suspend fun listenForMessages() {
            try {
                while (running) {
                    val length = input?.readInt() ?: break
                    val encrypted = ByteArray(length)
                    input?.readFully(encrypted)
                    val decrypted = CryptoEngine.decrypt(encrypted, secretKey!!)
                    // El wire format es: type|quotedId|quotedContent|text
                    val parts = decrypted.split("|", limit = 4)
                    val type = parts.getOrElse(0) { "chat" }
                    val quoteId = parts.getOrElse(1) { "" }.ifBlank { null }
                    val quoteContent = parts.getOrElse(2) { "" }.ifBlank { null }
                    val text = parts.getOrElse(3) { decrypted }
                    val message = MeshMessage(
                        content = text,
                        senderId = clientId,
                        type = type,
                        quotedMessageId = quoteId,
                        quotedMessageContent = quoteContent
                    )
                    _messages.emit(message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recibiendo mensaje: ${e.message}")
            } finally {
                disconnect()
            }
        }

        suspend fun send(message: MeshMessage) {
            try {
                // Construir wire format
                val wire = "${message.type}|${message.quotedMessageId ?: ""}|${message.quotedMessageContent ?: ""}|${message.content}"
                val encrypted = CryptoEngine.encrypt(wire, secretKey!!)
                output?.writeInt(encrypted.size)
                output?.write(encrypted)
                output?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando mensaje: ${e.message}")
            }
        }

        fun disconnect() {
            running = false
            try { socket.close() } catch (_: Exception) {}
            clients.remove(clientId)
            _connectedClientsCount.value = clients.size
        }
    }
}

data class MeshMessage(
    val content: String,
    val senderId: String = "self",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "chat",                // "chat", "delete", "reply"
    val quotedMessageId: String? = null,      // ID del mensaje citado
    val quotedMessageContent: String? = null  // Contenido del mensaje citado
)
