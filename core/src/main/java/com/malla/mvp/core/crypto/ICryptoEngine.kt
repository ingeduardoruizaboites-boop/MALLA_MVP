package com.malla.mvp.core.crypto

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

interface ICryptoEngine {
    fun generateKeyPair(): KeyPair
    fun publicKeyToBase64(publicKey: PublicKey): String
    fun base64ToPublicKey(base64: String): PublicKey
    fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): SecretKey
    fun encrypt(message: String, secretKey: SecretKey): ByteArray
    fun decrypt(data: ByteArray, secretKey: SecretKey): String
}
