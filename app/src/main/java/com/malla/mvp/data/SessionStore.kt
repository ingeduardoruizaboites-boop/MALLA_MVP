package com.malla.mvp.data

import com.malla.mvp.core.data.ISessionStore
import com.malla.mvp.data.dao.SessionDao

class SessionStore(private val dao: SessionDao?) : ISessionStore {
    override suspend fun getSession(contactId: String): ByteArray? = null
    override suspend fun saveSession(contactId: String, state: ByteArray) {}
}
