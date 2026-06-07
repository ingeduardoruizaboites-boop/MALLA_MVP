package com.malla.mvp.core.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.nio.ByteBuffer
import java.security.MessageDigest

object IdentityProofOfWork {

    data class IdentitySeal(
        val nonce: Int,
        val timestamp: Long,
        val hashHex: String
    )

    suspend fun generateSeal(pubKeyBytes: ByteArray): IdentitySeal =
        withContext(Dispatchers.Default) {
            val timestamp = System.currentTimeMillis()
            val digest = MessageDigest.getInstance("SHA-256")
            var nonce = 0

            while (isActive) {
                val input = buildInput(pubKeyBytes, nonce, timestamp)
                val hash = digest.digest(input)
                digest.reset()

                if (hash[0] == 0.toByte() &&
                    hash[1] == 0.toByte() &&
                    (hash[2].toInt() and 0xF0) == 0
                ) {
                    return@withContext IdentitySeal(
                        nonce = nonce,
                        timestamp = timestamp,
                        hashHex = bytesToHex(hash)
                    )
                }
                nonce++
                if (nonce % 1000 == 0) yield()
            }
            IdentitySeal(0, timestamp, "0".repeat(64))
        }

    fun validateSeal(pubKeyBytes: ByteArray, seal: IdentitySeal): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(buildInput(pubKeyBytes, seal.nonce, seal.timestamp))
        return hash[0] == 0.toByte() && hash[1] == 0.toByte() && (hash[2].toInt() and 0xF0) == 0
    }

    private fun buildInput(pubKey: ByteArray, nonce: Int, ts: Long): ByteArray {
        val buf = ByteBuffer.allocate(pubKey.size + 4 + 8)
        buf.put(pubKey)
        buf.putInt(nonce)
        buf.putLong(ts)
        return buf.array()
    }

    private fun bytesToHex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
}
