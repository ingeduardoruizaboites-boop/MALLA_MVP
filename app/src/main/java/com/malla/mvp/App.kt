package com.malla.mvp

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.malla.mvp.core.crypto.ICryptoEngine
import com.malla.mvp.crypto.CryptoEngineAdapter
import com.malla.mvp.core.util.IAppContext
import com.malla.mvp.di.Injector
import com.malla.mvp.identity.IdentityManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

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
        // Instalar handler de errores antes de cualquier inicialización
        val crashFile = File(filesDir, "crash.txt")
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            crashFile.writeText(sw.toString())
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        super.onCreate()
        context = this
        appContextProvider = this
        cryptoProvider = CryptoEngineAdapter()

        // Inicializar módulos (si fallan, el handler de arriba capturará el error para el siguiente inicio)
        try {
            // IdentityManager.init(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Error en IdentityManager: ${e.message}", Toast.LENGTH_LONG).show()
        }

        try {
            // Injector.init(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar servicios: ${e.message}", Toast.LENGTH_LONG).show()
        }

        try {
            // com.malla.mvp.ui.settings.ChatSettings.load(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Error en ChatSettings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
