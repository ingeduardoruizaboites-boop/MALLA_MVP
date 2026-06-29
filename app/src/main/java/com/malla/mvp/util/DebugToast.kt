package com.malla.mvp.util

import android.content.Context
import android.widget.Toast
import com.malla.mvp.core.engine.LogBuffer

fun Context.debugToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
    LogBuffer.add("TOAST", message)
}
