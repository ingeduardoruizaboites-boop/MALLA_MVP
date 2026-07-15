package com.malla.mvp.network
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.core.crypto.KeystoreManager
import kotlinx.coroutines.runBlocking
import com.malla.mvp.App
import com.malla.mvp.core.network.MeshMessage
import com.malla.mvp.core.engine.LogBuffer

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

    private var isServerRunning = false
    private val serverJob = Job()
    private val serverScope = CoroutineScope(Dispatchers.IO + serverJob)
    private val clients = mutableMapOf<String, ClientHandler>()
    private val localKeyPair = CryptoEngine.generateKeyPair()
    val localPublicKeyBase64 = CryptoEngine.publicKeyToBase64(localKeyPair.public)

    fun startServer() {
        if (isServerRunning) return
        isServerRunning = true
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
                isServerRunning = false
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

                // 1. Obtener clave de identidad local y firmar clave efímera
                val identityPubKeyB64 = IdentityManager.getPublicKeyBase64()
                    ?: throw Exception("Identidad local no disponible")
                val keystore = KeystoreManager(App.context)
                val signature = runBlocking { keystore.signData(localKeyPair.public.encoded) }
                val signatureB64 = android.util.Base64.encodeToString(signature, android.util.Base64.NO_WRAP)

                // 2. Enviar: claveEfímera|identidadPública|firma
                val localMsg = "$localPublicKeyBase64|$identityPubKeyB64|$signatureB64"
                output?.writeUTF(localMsg)
                output?.flush()

                // 3. Recibir mensaje del peer: claveEfímera|identidadPública|firma
                val peerMsg = input?.readUTF() ?: throw Exception("No se recibió handshake autenticado")
                val parts = peerMsg.split("|")
                if (parts.size != 3) throw Exception("Formato de handshake inválido")
                val peerEphemeralKeyB64 = parts[0]
                val peerIdentityKeyB64 = parts[1]
                val peerSignatureB64 = parts[2]

                // 4. Verificar firma de la clave efímera remota con su identidad
                val peerIdentityKey = CryptoEngine.base64ToPublicKey(peerIdentityKeyB64)
                val sigBytes = android.util.Base64.decode(peerSignatureB64, android.util.Base64.NO_WRAP)
                val sig = java.security.Signature.getInstance("SHA256withECDSA")
                sig.initVerify(peerIdentityKey)
                sig.update(CryptoEngine.base64ToPublicKey(peerEphemeralKeyB64).encoded)
                val valid = sig.verify(sigBytes)
                if (!valid) {
                    Log.e(TAG, "[NS:HS] Firma inválida de $clientId, rechazando")
                    LogBuffer.add("NS", "Handshake rechazado: firma inválida ${clientId}")
                    throw Exception("Firma de handshake inválida")
                }

                // 5. TOFU: verificar identidad guardada
                val prefs = App.context.getSharedPreferences("malla_tofu", android.content.Context.MODE_PRIVATE)
                val savedId = prefs.getString("identity_$clientId", null)
                if (savedId != null && savedId != peerIdentityKeyB64) {
                    Log.w(TAG, "[NS:HS] ALERTA: identidad de $clientId ha cambiado desde $savedId")
                    LogBuffer.add("NS", "TOFU WARNING: identidad cambiada para ${clientId}")
                    throw Exception("Identidad del peer ha cambiado (posible MITM)")
                } else if (savedId == null) {
                    prefs.edit().putString("identity_$clientId", peerIdentityKeyB64).apply()
                    Log.d(TAG, "[NS:HS] TOFU: primera conexión con $clientId, identidad guardada")
                }

                // 6. Inicializar DoubleRatchet con la clave efímera remota
                val peerPublicKey = CryptoEngine.base64ToPublicKey(peerEphemeralKeyB64)
                secretKey = CryptoEngine.deriveSharedSecret(localKeyPair.private, peerPublicKey)
                Log.d(TAG, "[NS:HS] Handshake autenticado completado con $clientId")
                LogBuffer.add("NS", "Handshake seguro OK: ${clientId}")
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
                    val type = if (parts.getOrElse(0) { "chat" } == "zumbido") 4 else 0
                    val quoteId = parts.getOrElse(1) { "" }.ifBlank { null }
                    val quoteContent = parts.getOrElse(2) { "" }.ifBlank { null }
                    val text = parts.getOrElse(3) { decrypted }
                    val message = MeshMessage(
                        content = text,
                        senderId = clientId,
                        type = type,
                    )
                    Log.d(TAG, "[NS:MSG] Mensaje recibido de $clientId (tipo=$type, ${encrypted.size} bytes)")
            LogBuffer.add("NS", "Mensaje recibido: tipo=${type}")
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
                val encrypted = CryptoEngine.encrypt(message.content, secretKey!!)
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

