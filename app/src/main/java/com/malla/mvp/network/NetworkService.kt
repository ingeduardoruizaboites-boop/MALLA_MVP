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
                Log.d(TAG, "[NS:TCP] Servidor iniciado en puerto $DEFAULT_PORT")
                while (isActive) {
                    val clientSocket = serverSocket.accept()
                    val handler = ClientHandler(clientSocket)
                    clients[handler.clientId] = handler
                    handler.start()
                    Log.d(TAG, "[NS:TCP] Nuevo cliente: ${handler.clientId} (total: ${clients.size})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[NS:ERR] Error en servidor: ${e.message}", e)
            }
        }
    }

    fun stopServer() {
        Log.d(TAG, "[NS:TCP] Deteniendo servidor (${clients.size} clientes)")
        serverJob.cancel()
        clients.values.forEach { it.disconnect() }
        clients.clear()
        _connectedClientsCount.value = 0
    }

    fun connectToPeer(address: String) {
        Log.d(TAG, "[NS:TCP] Intentando conectar a $address:$DEFAULT_PORT")
        serverScope.launch {
            try {
                val socket = Socket(address, DEFAULT_PORT)
                val handler = ClientHandler(socket)
                clients[handler.clientId] = handler
                handler.start()
                Log.d(TAG, "[NS:TCP] Conectado a $address (total: ${clients.size})")
            } catch (e: Exception) {
                Log.e(TAG, "[NS:ERR] Error conectando a $address: ${e.message}", e)
            }
        }
    }

    fun sendMessage(message: MeshMessage) {
        Log.d(TAG, "[NS:MSG] Enviando mensaje tipo=${message.type} a ${clients.size} clientes")
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
            Log.e(TAG, "[NS:ERR] Error obteniendo IP", e)
        }
        return "Desconocida"
    }

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
                Log.d(TAG, "[NS:HS] Handshake completado con $clientId")
                _connectedClientsCount.value = clients.size

                serverScope.launch {
                    listenForMessages()
                }
            } catch (e: Exception) {
                Log.e(TAG, "[NS:ERR] Handshake fallido con $clientId: ${e.message}", e)
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
                    Log.d(TAG, "[NS:MSG] Mensaje recibido de $clientId (tipo=$type, ${encrypted.size} bytes)")
                    _messages.emit(message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[NS:ERR] Error recibiendo mensaje de $clientId: ${e.message}", e)
            } finally {
                disconnect()
            }
        }

        suspend fun send(message: MeshMessage) {
            try {
                val wire = "${message.type}|${message.quotedMessageId ?: ""}|${message.quotedMessageContent ?: ""}|${message.content}"
                val encrypted = CryptoEngine.encrypt(wire, secretKey!!)
                output?.writeInt(encrypted.size)
                output?.write(encrypted)
                output?.flush()
                Log.d(TAG, "[NS:MSG] Mensaje enviado a $clientId (${encrypted.size} bytes cifrados)")
            } catch (e: Exception) {
                Log.e(TAG, "[NS:ERR] Error enviando mensaje a $clientId: ${e.message}", e)
            }
        }

        fun disconnect() {
            running = false
            try { socket.close() } catch (_: Exception) {}
            clients.remove(clientId)
            _connectedClientsCount.value = clients.size
            Log.d(TAG, "[NS:TCP] Cliente desconectado: $clientId (total: ${clients.size})")
        }
    }
}

data class MeshMessage(
    val content: String,
    val senderId: String = "self",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "chat",
    val quotedMessageId: String? = null,
    val quotedMessageContent: String? = null
)
