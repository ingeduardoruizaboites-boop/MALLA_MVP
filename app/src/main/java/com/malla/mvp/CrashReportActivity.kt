package com.malla.mvp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorText = intent.getStringExtra("error") ?: "Error desconocido"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "⚠️ Error inesperado"
            textSize = 20f
        }
        layout.addView(title)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        val textView = TextView(this).apply {
            text = errorText
            textSize = 12f
        }
        scroll.addView(textView)
        layout.addView(scroll)

        val retryButton = Button(this).apply {
            text = "Reintentar"
            setOnClickListener {
                // Borrar el archivo de crash para que no vuelva a aparecer
                File(filesDir, "crash.txt").delete()
                // Reiniciar la app recreando la actividad principal
                val intent = Intent(this@CrashReportActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        layout.addView(retryButton)

        setContentView(layout)
    }
}
