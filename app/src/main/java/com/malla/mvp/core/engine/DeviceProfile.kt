package com.malla.mvp.core.engine

import android.app.ActivityManager
import android.content.Context
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object DeviceProfile {
    var forwardLimit: Int = 5
    suspend fun initialize(context: Context) {
        val memInfo = ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(memInfo)
        val totalRamGB = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        forwardLimit = when {
            totalRamGB < 2.0 -> 0
            totalRamGB < 4.0 -> 5
            else -> 15
        }
    }
}
