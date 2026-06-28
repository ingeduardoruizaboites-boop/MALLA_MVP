package com.malla.mvp.core.ultrasound

import kotlinx.coroutines.flow.Flow

interface IUltrasoundManager {
    fun startBroadcasting(code: String): Boolean
    fun stopBroadcasting()
    fun startListening(): Flow<String>
    fun stopListening()
    fun isAvailable(): Boolean
}
