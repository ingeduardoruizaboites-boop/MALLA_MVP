package com.malla.mvp.network

import android.content.Context
import android.content.SharedPreferences
import com.malla.mvp.App
import com.malla.mvp.core.engine.LogBuffer

object SeedManager {
    private const val PREFS_NAME = "malla_seeds"
    private const val MAX_SEEDS = 10
    private val seeds = mutableListOf<String>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs?.getString("seeds", "") ?: ""
        if (saved.isNotBlank()) {
            seeds.clear()
            seeds.addAll(saved.split(",").filter { it.isNotBlank() })
        }
        LogBuffer.add("SEED", "Semillas cargadas: ${seeds.size}")
    }

    fun publishMySeed(ip: String, port: Int) {
        val entry = "$ip:$port"
        if (!seeds.contains(entry)) {
            seeds.add(entry)
            if (seeds.size > MAX_SEEDS) seeds.removeAt(0)
            saveSeeds()
            LogBuffer.add("SEED", "Semilla publicada: $entry")
        }
    }

    fun getSeeds(): List<String> = seeds.toList()

    fun exchangeSeeds(otherSeeds: List<String>) {
        otherSeeds.forEach { seed ->
            if (!seeds.contains(seed) && seeds.size < MAX_SEEDS) {
                seeds.add(seed)
                LogBuffer.add("SEED", "Semilla recibida: $seed")
            }
        }
        saveSeeds()
    }

    private fun saveSeeds() {
        prefs?.edit()?.putString("seeds", seeds.joinToString(","))?.apply()
    }
}
