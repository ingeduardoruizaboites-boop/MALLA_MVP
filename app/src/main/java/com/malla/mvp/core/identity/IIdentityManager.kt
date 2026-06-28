package com.malla.mvp.core.identity

interface IIdentityManager {
    fun getMyId(): String
    fun getMyName(): String
    fun getMyPublicKeyBase64(): String?
    fun getMyPhone(): String?
    fun signChallenge(challenge: ByteArray): ByteArray?
    fun verifySignature(publicKeyBase64: String, challenge: ByteArray, signature: ByteArray): Boolean
}
