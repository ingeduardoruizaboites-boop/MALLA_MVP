package com.malla.mvp.core.crypto

import android.util.Base64
import com.malla.mvp.identity.IdentityManager

object IdentityQrPayload {

    private const val PREFIX = "MALLA:"
    private const val VALIDITY_SECONDS = 60L

    data class ParsedPayload(
        val pubKeyBase64: String,
        val timestamp: Long,
        val signatureBase64: String
    )

    // Genera el contenido del QR: "MALLA:<pubKey>:<timestamp>:<firma>"
    suspend fun generate(keystoreManager: KeystoreManager, identityManager: IdentityManager): String {
        val pubKeyBase64 = identityManager.getPublicKeyBase64()
            ?: throw IllegalStateException("No hay identidad creada")
        val timestamp = System.currentTimeMillis()
        val dataToSign = "$pubKeyBase64:$timestamp".toByteArray(Charsets.UTF_8)
        val signature = keystoreManager.signData(dataToSign)
        val signatureBase64 = Base64.encodeToString(signature, Base64.NO_WRAP)
        return "$PREFIX$pubKeyBase64:$timestamp:$signatureBase64"
    }

    // Parsea y verifica el contenido del QR
    fun parseAndVerify(qrContent: String): ParsedPayload? {
        if (!qrContent.startsWith(PREFIX)) return null
        val parts = qrContent.removePrefix(PREFIX).split(":")
        if (parts.size != 3) return null
        val pubKeyBase64 = parts[0]
        val timestamp = parts[1].toLongOrNull() ?: return null
        val signatureBase64 = parts[2]

        // Verificar vigencia (60 segundos)
        val now = System.currentTimeMillis()
        if (now - timestamp > VALIDITY_SECONDS * 1000) return null

        // Verificar firma con la PubKey recibida
        val dataToVerify = "$pubKeyBase64:$timestamp".toByteArray(Charsets.UTF_8)
        val signature = Base64.decode(signatureBase64, Base64.NO_WRAP)
        if (!verifySignature(pubKeyBase64, dataToVerify, signature)) return null

        return ParsedPayload(pubKeyBase64, timestamp, signatureBase64)
    }

    private fun verifySignature(pubKeyBase64: String, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            val publicKey = com.malla.mvp.crypto.CryptoEngine.base64ToPublicKey(pubKeyBase64)
            val sig = java.security.Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }
}
