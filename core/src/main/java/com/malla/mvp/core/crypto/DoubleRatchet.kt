package com.malla.mvp.core.crypto

import java.security.KeyPair

class DoubleRatchet(
    private val localKeyPair: KeyPair,
    private val remotePublicKey: java.security.PublicKey,
    private val contactId: String
) {
    fun encrypt(plaintext: String): String = plaintext
    fun decrypt(ciphertext: String): String = ciphertext
    fun exportState(): ByteArray = ByteArray(0)
    companion object {
        fun restoreState(state: ByteArray, localKeyPair: KeyPair, remotePublicKey: java.security.PublicKey, contactId: String): DoubleRatchet {
            return DoubleRatchet(localKeyPair, remotePublicKey, contactId)
        }
    }
}
