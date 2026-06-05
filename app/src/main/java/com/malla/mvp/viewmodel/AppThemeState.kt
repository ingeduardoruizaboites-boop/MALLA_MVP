package com.malla.mvp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.malla.mvp.network.ConnectivityMonitor
import com.malla.mvp.ui.theme.MallaColorScheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Estado reactivo del tema de la app.
 * Combina la preferencia del usuario con el estado de conectividad
 * para forzar el tema OLED en modo mesh (máxima eficiencia).
 */
class AppThemeState private constructor(private val prefs: SharedPreferences?) {

    companion object {
        private const val KEY_SCHEME_NAME = "scheme_name"

        fun create(context: Context): AppThemeState {
            val prefs = try {
                context.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                Log.e("AppThemeState", "Error accediendo a SharedPreferences", e)
                null
            }
            return AppThemeState(prefs)
        }

        fun createFallback(): AppThemeState = AppThemeState(null)

        /** Lista de todos los temas disponibles para el selector */
        val availableSchemes = MallaColorScheme.ALL
    }

    // Tema seleccionado por el usuario (persistido)
    private val _userSelectedScheme = MutableStateFlow(loadScheme())
    val userSelectedScheme: StateFlow<MallaColorScheme> = _userSelectedScheme

    // Tema efectivo combinando preferencia + conectividad
    private val _effectiveScheme = MutableStateFlow(MallaColorScheme.MALLA_DARK)
    val currentTheme: StateFlow<MallaColorScheme> = _effectiveScheme

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        scope.launch {
            combine(_userSelectedScheme, ConnectivityMonitor.isOnline) { userScheme, isOnline ->
                if (isOnline) userScheme else MallaColorScheme.OLED_PURE
            }.collect { scheme ->
                _effectiveScheme.value = scheme
            }
        }
    }

    /** Cambiar tema seleccionado por el usuario */
    fun selectScheme(scheme: MallaColorScheme) {
        _userSelectedScheme.value = scheme
        saveScheme(scheme)
    }

    private fun loadScheme(): MallaColorScheme {
        val name = prefs?.getString(KEY_SCHEME_NAME, MallaColorScheme.MALLA_DARK.name)
            ?: MallaColorScheme.MALLA_DARK.name
        return MallaColorScheme.ALL.find { it.name == name } ?: MallaColorScheme.MALLA_DARK
    }

    private fun saveScheme(scheme: MallaColorScheme) {
        try {
            prefs?.edit()?.putString(KEY_SCHEME_NAME, scheme.name)?.apply()
        } catch (e: Exception) {
            Log.e("AppThemeState", "Error guardando tema", e)
        }
    }
}
