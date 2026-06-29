package com.malla.mvp.network

import com.malla.mvp.core.ultrasound.IUltrasoundManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class UltrasoundManager : IUltrasoundManager {
    override fun startBroadcasting(code: String): Boolean = true
    override fun stopBroadcasting() {}
    override fun startListening(): Flow<String> = emptyFlow()
    override fun stopListening() {}
    override fun isAvailable(): Boolean = true
}
