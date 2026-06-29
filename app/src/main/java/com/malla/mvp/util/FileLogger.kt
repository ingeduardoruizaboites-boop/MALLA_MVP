package com.malla.mvp.util

import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {
    private var writer: PrintWriter? = null

    fun init() {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "malla_debug.log")
            writer = PrintWriter(FileWriter(file, true), true)
            log("SYSTEM", "FileLogger iniciado")
        } catch (_: Exception) {}
    }

    fun log(tag: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        try { writer?.println("[$timestamp] $tag: $message") } catch (_: Exception) {}
    }

    fun close() { try { writer?.close() } catch (_: Exception) {} }
}
