package com.malla.mvp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MeshChatService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
