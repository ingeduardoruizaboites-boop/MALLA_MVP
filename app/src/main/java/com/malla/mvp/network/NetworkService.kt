package com.malla.mvp.network
import com.malla.mvp.identity.IdentityManager
import com.malla.mvp.core.crypto.KeystoreManager
import kotlinx.coroutines.runBlocking
import com.malla.mvp.App
import com.malla.mvp.core.network.MeshMessage
import com.malla.mvp.core.engine.LogBuffer

import com.malla.mvp.crypto.CryptoEngine
import com.malla.mvp.core.crypto.DoubleRatchet
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
    private val identityToClient = mutableMapOf<String, String>()
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

    fun sendMessageTo(recipientId: String, message: MeshMessage) {
        val clientId = identityToClient[recipientId]
        if (clientId == null) {
            Log.e(TAG, "[NS:MSG] No hay conexión activa con $recipientId")
            return
        }
        val handler = clients[clientId]
        if (handler == null) {
            Log.e(TAG, "[NS:MSG] Cliente $recipientId no encontrado en conexiones")
            return
        }
        Log.d(TAG, "[NS:MSG] Enviando mensaje a $recipientId (client: $clientId)")
        serverScope.launch {
            handler.send(message)
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
        private var ratchet: DoubleRatchet? = null
        private var running = false
        // Caché de mensajes-clave saltados (contador -> clave) para tolerar desorden
        private val skippedKeys = mutableMapOf<Int, ByteArray>()

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
                // Actualizar mapeo de identidad siempre (reconexiones)
                identityToClient[peerIdentityKeyB64] = clientId

                // 6. Inicializar DoubleRatchet con la clave efímera remota
                val peerPublicKey = CryptoEngine.base64ToPublicKey(peerEphemeralKeyB64)
                // Intentar restaurar estado anterior del ratchet
                val savedStateB64 = App.context.getSharedPreferences("ratchet_state", android.content.Context.MODE_PRIVATE)
                    .getString("state_$clientId", null)
                if (savedStateB64 != null) {
                    try {
                        val savedState = android.util.Base64.decode(savedStateB64, android.util.Base64.NO_WRAP)
                        ratchet = DoubleRatchet.restoreState(savedState, localKeyPair, peerPublicKey, clientId)
                        Log.d(TAG, "[NS:HS] Ratchet restaurado para $clientId")
                    } catch (e: Exception) {
                        Log.w(TAG, "[NS:HS] No se pudo restaurar ratchet, iniciando nuevo: ${e.message}")
                        ratchet = DoubleRatchet(localKeyPair, peerPublicKey, clientId)
                    }
                } else {
                    ratchet = DoubleRatchet(localKeyPair, peerPublicKey, clientId)
                }
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
                    // Leer si es un mensaje de control o de datos
                    val header = input?.readUTF() ?: break
                    if (header.startsWith("RATCHET_STEP|")) {
                        handleRatchetStep(header.removePrefix("RATCHET_STEP|"))
                        continue
                    }
                    // Si no es control, asumimos que es longitud del mensaje (compatibilidad)
                    val length = try { header.toInt() } catch (e: NumberFormatException) { break }
                    val encrypted = ByteArray(length)
                    input?.readFully(encrypted)
                    val currentRatchet = ratchet ?: break
                    val decryptedBase64 = String(encrypted, Charsets.UTF_8)
                    // Intentar descifrar con la clave actual; si falla, buscar en caché de saltados
                    val decrypted = try {
                        currentRatchet.decrypt(decryptedBase64)
                    } catch (e: Exception) {
                        // Intentar con claves saltadas almacenadas (desorden)
                        var recovered: String? = null
                        val skippedEntries = skippedKeys.entries.sortedBy { it.key }
                        for ((counter, key) in skippedEntries) {
                            try {
                                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                                val iv = android.util.Base64.decode(decryptedBase64, android.util.Base64.NO_WRAP).copyOfRange(0, 12)
                                val encryptedData = android.util.Base64.decode(decryptedBase64, android.util.Base64.NO_WRAP).copyOfRange(12, encrypted.size)
                                val keySpec = javax.crypto.spec.SecretKeySpec(key, "AES")
                                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, javax.crypto.spec.GCMParameterSpec(128, iv))
                                recovered = String(cipher.doFinal(encryptedData), Charsets.UTF_8)
                                skippedKeys.remove(counter)
                                break
                            } catch (_: Exception) { }
                        }
                        recovered ?: throw e
                    }

                    val parts = decrypted.split("|", limit = 4)
                    val type = if (parts.getOrElse(0) { "chat" } == "zumbido") 4 else 0
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
                val currentRatchet = ratchet ?: return
                // Realizar ratchetStep cada 10 mensajes (o por tiempo)
                if (messageCount++ % 10 == 0) {
                    val newPubKey = currentRatchet.ratchetStep()
                    // Enviar la nueva clave pública al peer
                    val newPubKeyB64 = android.util.Base64.encodeToString(newPubKey.encoded, android.util.Base64.NO_WRAP)
                    output?.writeUTF("RATCHET_STEP|$newPubKeyB64")
                    output?.flush()
                    // Guardar estado del ratchet
                    App.context.getSharedPreferences("ratchet_state", android.content.Context.MODE_PRIVATE)
                        .edit().putString("state_$clientId", android.util.Base64.encodeToString(currentRatchet.exportState(), android.util.Base64.NO_WRAP)).apply()
                    Log.d(TAG, "[NS:RATCHET] Ratchet step enviado a $clientId")
                }

                val encryptedBase64 = currentRatchet.encrypt(message.content)
                val encrypted = encryptedBase64.toByteArray(Charsets.UTF_8)
                output?.writeInt(encrypted.size)
                output?.write(encrypted)
                output?.flush()
                Log.d(TAG, "[NS:MSG] Mensaje enviado a $clientId (${encrypted.size} bytes cifrados con DoubleRatchet)")
            } catch (e: Exception) {
                Log.e(TAG, "[NS:ERR] Error enviando mensaje a $clientId: ${e.message}", e)
            }
        }

        // Contador de mensajes para decidir cuándo hacer ratchetStep
        private var messageCount = 0

        // Procesar mensajes de control del ratchet (nueva clave pública del peer)
        private fun handleRatchetStep(newPubKeyB64: String) {
            try {
                val currentRatchet = ratchet ?: return
                val newPubKey = CryptoEngine.base64ToPublicKey(newPubKeyB64)
                currentRatchet.receiveRemoteRatchetKey(newPubKey)
                // Guardar estado tras recibir ratchet step
                App.context.getSharedPreferences("ratchet_state", android.content.Context.MODE_PRIVATE)
                    .edit().putString("state_$clientId", android.util.Base64.encodeToString(currentRatchet.exportState(), android.util.Base64.NO_WRAP)).apply()
                Log.d(TAG, "[NS:RATCHET] Ratchet step recibido de $clientId")
            } catch (e: Exception) {
                Log.e(TAG, "[NS:RATCHET] Error procesando ratchet step: ${e.message}", e)
            }
        }

        fun disconnect() {
            running = false
            try { socket.close() } catch (_: Exception) {}
            clients.remove(clientId)
            identityToClient.values.removeAll { it == clientId }
            _connectedClientsCount.value = clients.size
            Log.d(TAG, "[NS:TCP] Cliente desconectado: $clientId (total: ${clients.size})")
        }
    }
}

