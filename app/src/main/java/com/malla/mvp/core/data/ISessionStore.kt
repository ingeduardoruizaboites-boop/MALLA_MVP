package com.malla.mvp.core.data

interface ISessionStore {
    suspend fun getSession(contactId: String): ByteArray?
    suspend fun saveSession(contactId: String, state: ByteArray)
}
