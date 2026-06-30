package com.malla.mvp.transport

import com.malla.mvp.core.transport.ITransport
import com.malla.mvp.core.transport.TransportState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import javax.crypto.SecretKey
import com.malla.mvp.crypto.CryptoEngine
import android.util.Log

class TcpDirectTransport : ITransport {
    override val type = "TCP_DIRECT"
    private val _state = MutableStateFlow<TransportState>(TransportState.Idle)
    override val state: Flow<TransportState> = _state
    override val isAuthenticated: Flow<Boolean> = MutableStateFlow(false)
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var secretKey: SecretKey? = null
    private var running = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val localKeyPair = CryptoEngine.generateKeyPair()
    private val localPublicKeyBase64 = CryptoEngine.publicKeyToBase64(localKeyPair.public)

    fun startServer() {
        if (running) return
        running = true
        scope.launch {
            try {
                _state.value = TransportState.Connecting
                serverSocket = ServerSocket(8889)
                Log.d("TcpDirect", "Servidor TCP iniciado en 8889")
                while (running) {
                    val socket = serverSocket!!.accept()
                    clientSocket = socket
                    performHandshake(socket)
                    _state.value = TransportState.Connected
                    listenIncoming()
                }
            } catch (e: Exception) {
                _state.value = TransportState.Error(e.message ?: "Error")
            }
        }
    }

    override suspend fun connect(address: String) {
        withContext(Dispatchers.IO) {
            try {
                _state.value = TransportState.Connecting
                clientSocket = Socket(address, 8889)
                performHandshake(clientSocket!!)
                _state.value = TransportState.Connected
                scope.launch { listenIncoming() }
            } catch (e: Exception) {
                _state.value = TransportState.Error(e.message ?: "Error")
            }
        }
    }

    override suspend fun send(payload: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                val encrypted = CryptoEngine.encrypt(String(payload), secretKey!!)
                output?.writeInt(encrypted.size)
                output?.write(encrypted)
                output?.flush()
            } catch (e: Exception) {
                _state.value = TransportState.Error(e.message ?: "Error enviando")
            }
        }
    }

    override suspend fun disconnect() {
        running = false
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        _state.value = TransportState.Idle
    }

    private fun performHandshake(socket: Socket) {
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())
        output?.writeUTF(localPublicKeyBase64)
        output?.flush()
        val peerPubKeyBase64 = input?.readUTF() ?: throw Exception("No se recibió clave pública")
        val peerPublicKey = CryptoEngine.base64ToPublicKey(peerPubKeyBase64)
        secretKey = CryptoEngine.deriveSharedSecret(localKeyPair.private, peerPublicKey)
    }

    private suspend fun listenIncoming() {
        try {
            while (running) {
                val length = input?.readInt() ?: break
                val encrypted = ByteArray(length)
                input?.readFully(encrypted)
                val decrypted = CryptoEngine.decrypt(encrypted, secretKey!!)
                // Emitir al flujo de mensajes (si se necesita en el futuro)
            }
        } catch (e: Exception) {
            if (running) _state.value = TransportState.Error(e.message ?: "Error")
        }
    }
}
