package com.malla.mvp.core.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class KeystoreManager(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "malla_identity_key_v1"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val AUTH_VALIDITY_SECONDS = 30
    }

    fun keyExists(): Boolean {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            ks.containsAlias(KEY_ALIAS)
        } catch (e: Exception) { false }
    }

    suspend fun generateIdentityKey(
        useBiometric: Boolean = true
    ): ByteArray = withContext(Dispatchers.Default) {
        if (keyExists()) return@withContext getPublicKeyBytes()

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .apply {
                if (useBiometric) {
                    setUserAuthenticationRequired(true)
                    setUserAuthenticationParameters(
                        AUTH_VALIDITY_SECONDS,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                    )
                }
            }
            .build()

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )
        generator.initialize(spec)
        val keyPair = generator.generateKeyPair()
        keyPair.public.encoded
    }

    fun getPublicKeyBytes(): ByteArray {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return ks.getCertificate(KEY_ALIAS)?.publicKey?.encoded
            ?: throw IllegalStateException("No hay llave en Keystore")
    }

    suspend fun signData(data: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val privateKey = ks.getKey(KEY_ALIAS, null) as? PrivateKey
                ?: throw IllegalStateException("Llave privada no encontrada")

            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initSign(privateKey)
            sig.update(data)
            sig.sign()
        }

    suspend fun derivePersonalSalt(): ByteArray {
        val input = "MALLA_CONTACT_SALT_V1".toByteArray(Charsets.UTF_8)
        return signData(input)
    }

    fun deleteKey(): Boolean {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            ks.deleteEntry(KEY_ALIAS)
            true
        } catch (e: Exception) { false }
    }
}
