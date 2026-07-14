package com.malla.mvp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import java.io.File

class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val log = File(filesDir, "crash.txt").readText()
        val tv = TextView(this)
        tv.text = log
        tv.textSize = 12f
        tv.setPadding(20, 40, 20, 20)
        setContentView(tv)
    }
}
