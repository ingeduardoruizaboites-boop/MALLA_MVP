package com.malla.mvp.core.crypto

import java.security.*
import javax.crypto.*
import javax.crypto.spec.*
import android.util.Base64
import javax.crypto.Mac
import java.security.spec.X509EncodedKeySpec
import java.security.spec.ECGenParameterSpec
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class DoubleRatchet(
    private var localEphemeralKeyPair: KeyPair,
    private var remoteEphemeralPublicKey: PublicKey,
    private val contactId: String,
    rootKeyInput: ByteArray? = null
) {
    companion object {
        private const val HMAC_ALG = "HmacSHA256"
        private const val AES_ALG = "AES/GCM/NoPadding"
        private const val MESSAGE_KEY_LEN = 32
        private const val ROOT_KEY_LEN = 32
        private const val CHAIN_KEY_LEN = 32

        fun restoreState(
            state: ByteArray,
            localKeyPair: KeyPair,
            remotePublicKey: PublicKey,
            contactId: String
        ): DoubleRatchet {
            val dis = DataInputStream(ByteArrayInputStream(state))
            val rootKeyLen = dis.readInt()
            val rootKey = ByteArray(rootKeyLen)
            dis.readFully(rootKey)
            val sendChainKeyLen = dis.readInt()
            val sendChainKey = ByteArray(sendChainKeyLen)
            dis.readFully(sendChainKey)
            val recvChainKeyLen = dis.readInt()
            val recvChainKey = ByteArray(recvChainKeyLen)
            dis.readFully(recvChainKey)
            val sendCount = dis.readInt()
            val recvCount = dis.readInt()
            val dr = DoubleRatchet(localKeyPair, remotePublicKey, contactId, rootKey)
            dr.sendChainKey = sendChainKey
            dr.recvChainKey = recvChainKey
            dr.sendCount = sendCount
            dr.recvCount = recvCount
            return dr
        }
    }

    // Ratchet state
    private var rootKey: ByteArray
    private var sendChainKey: ByteArray
    private var recvChainKey: ByteArray
    private var sendCount = 0
    private var recvCount = 0
    val skippedKeys = mutableMapOf<Int, ByteArray>()

    init {
        if (rootKeyInput != null) {
            rootKey = rootKeyInput
        } else {
            // Derive root key from shared secret ECDH
            val sharedSecret = computeSharedSecret(localEphemeralKeyPair.private, remoteEphemeralPublicKey)
            rootKey = hkdf(sharedSecret, null, "MALLA_ROOT_KEY".toByteArray(), ROOT_KEY_LEN)
        }
        // Derive initial chain keys from root key
        val chainKeys = hkdf(rootKey, null, "MALLA_CHAIN_KEYS".toByteArray(), CHAIN_KEY_LEN * 2)
        sendChainKey = chainKeys.copyOfRange(0, CHAIN_KEY_LEN)
        recvChainKey = chainKeys.copyOfRange(CHAIN_KEY_LEN, CHAIN_KEY_LEN * 2)
    }

    /**
     * Encrypt plaintext and return Base64-encoded ciphertext (IV + encrypted data).
     * After encryption, advances the send chain key (ratchet symmetric step).
     */
    fun encrypt(plaintext: String): String {
        val messageKey = deriveMessageKey(sendChainKey, sendCount)
        sendChainKey = advanceChainKey(sendChainKey)
        sendCount++
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(AES_ALG)
        val keySpec = SecretKeySpec(messageKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val result = iv + encrypted
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    /**
     * Decrypt Base64-encoded ciphertext and return plaintext.
     * After decryption, advances the receive chain key (ratchet symmetric step).
     */
    fun decrypt(ciphertextBase64: String): String {
        val messageKey = deriveMessageKey(recvChainKey, recvCount)
        try {
            val plaintext = tryDecrypt(ciphertextBase64, messageKey)
            recvChainKey = advanceChainKey(recvChainKey)
            recvCount++
            return plaintext
        } catch (e: Exception) {
            // Intentar con claves saltadas almacenadas
            for ((counter, key) in skippedKeys.entries.sortedBy { it.key }) {
                try {
                    val plaintext = tryDecrypt(ciphertextBase64, key)
                    skippedKeys.remove(counter)
                    return plaintext
                } catch (_: Exception) {}
            }
            throw e
        }
    }

    /**
     * Perform a new ephemeral key exchange (ratchet asymmetric step).
     * Call this when sending a new message to rotate the ratchet.
     * Returns the new local ephemeral public key (must be transmitted to the other party).
     */
    fun ratchetStep(): PublicKey {
        // Generate new ephemeral key pair
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val newKeyPair = kpg.generateKeyPair()
        val sharedSecret = computeSharedSecret(newKeyPair.private, remoteEphemeralPublicKey)
        // Update root key with new shared secret
        rootKey = hkdf(rootKey, sharedSecret, "MALLA_RATCHET_STEP".toByteArray(), ROOT_KEY_LEN)
        // Derive new chain keys
        val chainKeys = hkdf(rootKey, null, "MALLA_CHAIN_KEYS".toByteArray(), CHAIN_KEY_LEN * 2)
        sendChainKey = chainKeys.copyOfRange(0, CHAIN_KEY_LEN)
        recvChainKey = chainKeys.copyOfRange(CHAIN_KEY_LEN, CHAIN_KEY_LEN * 2)
        sendCount = 0
        recvCount = 0
        localEphemeralKeyPair = newKeyPair
        return newKeyPair.public
    }

    /**
     * Receive a new remote ephemeral public key (called when the other party performs a ratchet step).
     */
    fun receiveRemoteRatchetKey(newRemotePublicKey: PublicKey) {
        val sharedSecret = computeSharedSecret(localEphemeralKeyPair.private, newRemotePublicKey)
        rootKey = hkdf(rootKey, sharedSecret, "MALLA_RATCHET_STEP".toByteArray(), ROOT_KEY_LEN)
        val chainKeys = hkdf(rootKey, null, "MALLA_CHAIN_KEYS".toByteArray(), CHAIN_KEY_LEN * 2)
        sendChainKey = chainKeys.copyOfRange(0, CHAIN_KEY_LEN)
        recvChainKey = chainKeys.copyOfRange(CHAIN_KEY_LEN, CHAIN_KEY_LEN * 2)
        sendCount = 0
        recvCount = 0
        remoteEphemeralPublicKey = newRemotePublicKey
    }

    fun exportState(): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.writeInt(rootKey.size)
        dos.write(rootKey)
        dos.writeInt(sendChainKey.size)
        dos.write(sendChainKey)
        dos.writeInt(recvChainKey.size)
        dos.write(recvChainKey)
        dos.writeInt(sendCount)
        dos.writeInt(recvCount)
        return baos.toByteArray()
    }

    private fun computeSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        return ka.generateSecret()
    }

    private fun deriveMessageKey(chainKey: ByteArray, counter: Int): ByteArray {
        val mac = Mac.getInstance(HMAC_ALG)
        mac.init(SecretKeySpec(chainKey, HMAC_ALG))
        mac.update(0x01.toByte())
        mac.update(counter.toByte()) // simplified, should use full counter encoding
        return mac.doFinal().copyOf(MESSAGE_KEY_LEN)
    }

    private fun advanceChainKey(chainKey: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALG)
        mac.init(SecretKeySpec(chainKey, HMAC_ALG))
        mac.update(0x02.toByte())
        return mac.doFinal().copyOf(CHAIN_KEY_LEN)
    }

    private fun hkdf(
        salt: ByteArray?,
        ikm: ByteArray?,
        info: ByteArray,
        length: Int
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_ALG)
        val saltKey = if (salt != null && salt.isNotEmpty()) {
            SecretKeySpec(salt, HMAC_ALG)
        } else {
            SecretKeySpec(ByteArray(32) { 0 }, HMAC_ALG)
        }
        mac.init(saltKey)
        val prk = mac.doFinal(ikm ?: ByteArray(0))
        mac.init(SecretKeySpec(prk, HMAC_ALG))
        val t = ByteArrayOutputStream()
        var last = ByteArray(0)
        while (t.size() < length) {
            mac.update(last)
            mac.update(info)
            mac.update((t.size() / 32 + 1).toByte())
            last = mac.doFinal()
            t.write(last)
        }
        return t.toByteArray().copyOf(length)
    }

    fun fillSkippedKeys(count: Int = 5) {
        var chainKey = recvChainKey.copyOf()
        var counter = recvCount
        repeat(count) {
            val key = deriveMessageKey(chainKey, counter)
            skippedKeys[counter] = key
            chainKey = advanceChainKey(chainKey)
            counter++
        }
    }
    private fun tryDecrypt(ciphertextBase64: String, key: ByteArray): String {
        val data = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
