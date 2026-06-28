package com.malla.mvp

import android.app.Application
import android.widget.Toast
import com.malla.mvp.core.crypto.ICryptoEngine
import com.malla.mvp.crypto.CryptoEngineAdapter
import com.malla.mvp.core.util.IAppContext
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
        super.onCreate()
        context = this
        appContextProvider = this
        cryptoProvider = CryptoEngineAdapter()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString().take(500)
            Toast.makeText(context, "Error: ${throwable.message}\n$stackTrace", Toast.LENGTH_LONG).show()
            Thread.sleep(4000)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
