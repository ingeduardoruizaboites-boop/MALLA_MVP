package com.malla.mvp.crypto

import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.*
import java.security.MessageDigest
import android.util.Base64

object CryptoEngine {
    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun publicKeyToBase64(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun base64ToPublicKey(base64: String): PublicKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(keySpec)
    }

    fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): SecretKey {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        val sharedSecret = ka.generateSecret()
        // Derivar clave AES-256 usando SHA-256 (Corrección 1 de la guía V3.0)
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(sharedSecret)  // 32 bytes para AES-256
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(message: String, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return iv + encrypted   // IV concatenado con datos
    }

    fun decrypt(data: ByteArray, secretKey: SecretKey): String {
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }
}
