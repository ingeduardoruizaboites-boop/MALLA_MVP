package com.malla.mvp.crypto

import com.malla.mvp.core.crypto.ICryptoEngine
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

class CryptoEngineAdapter : ICryptoEngine {
    override fun generateKeyPair(): KeyPair = CryptoEngine.generateKeyPair()
    override fun publicKeyToBase64(publicKey: PublicKey): String = CryptoEngine.publicKeyToBase64(publicKey)
    override fun base64ToPublicKey(base64: String): PublicKey = CryptoEngine.base64ToPublicKey(base64)
    override fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): SecretKey = CryptoEngine.deriveSharedSecret(privateKey, publicKey)
    override fun encrypt(message: String, secretKey: SecretKey): ByteArray = CryptoEngine.encrypt(message, secretKey)
    override fun decrypt(data: ByteArray, secretKey: SecretKey): String = CryptoEngine.decrypt(data, secretKey)
}
