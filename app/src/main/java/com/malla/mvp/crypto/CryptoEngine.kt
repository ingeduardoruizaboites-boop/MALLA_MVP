package com.malla.mvp.crypto

import java.security.*
import java.security.spec.*
import javax.crypto.*
import javax.crypto.spec.*
import android.util.Base64

object CryptoEngine {
    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }
    fun publicKeyToBase64(publicKey: PublicKey): String = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    fun base64ToPublicKey(base64: String): PublicKey {
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(X509EncodedKeySpec(Base64.decode(base64, Base64.NO_WRAP)))
    }
    fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): SecretKey {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        val digest = MessageDigest.getInstance("SHA-256").digest(ka.generateSecret())
        return SecretKeySpec(digest, "AES")
    }
    fun encrypt(message: String, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return iv + encrypted
    }
    fun decrypt(data: ByteArray, secretKey: SecretKey): String {
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
