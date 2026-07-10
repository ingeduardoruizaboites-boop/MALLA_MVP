package com.malla.mvp

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.malla.mvp.core.crypto.ICryptoEngine
import com.malla.mvp.crypto.CryptoEngineAdapter
import com.malla.mvp.core.util.IAppContext
import com.malla.mvp.di.Injector
import java.io.File

class App : Application(), IAppContext {
    companion object {
        lateinit var context: Application
            private set
        lateinit var appContextProvider: IAppContext
            private set
        lateinit var cryptoProvider: ICryptoEngine
            private set
    }

    override fun getContext(): android.content.Context = this

    override fun onCreate() {
        super.onCreate()
        context = this
        appContextProvider = this
        cryptoProvider = CryptoEngineAdapter()
        
        // 👇 LÍNEA FALTANTE
        Injector.init(this)
        com.malla.mvp.ui.settings.ChatSettings.load(this)

        val mainHandler = Handler(Looper.getMainLooper())
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val msg = throwable.message ?: "Error desconocido"
            // Guardar en archivo para el siguiente inicio
            try {
                File(filesDir, "crash.txt").writeText(msg)
            } catch (_: Exception) {}
            
            mainHandler.post {
                Toast.makeText(this, "Error fatal: $msg", Toast.LENGTH_LONG).show()
            }
            // Dar tiempo para ver el Toast antes de matar la app
            mainHandler.postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
            }, 3000)
        }
    }
}
