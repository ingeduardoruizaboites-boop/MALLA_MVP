package com.malla.mvp.core.engine

import android.content.Context
import com.malla.mvp.App

object PremiumManager {
    var isPremium: Boolean = false
    fun init() {
        val prefs = App.context.getSharedPreferences("malla_prefs", Context.MODE_PRIVATE)
        isPremium = prefs.getBoolean("premium_enabled", false)
    }
}
