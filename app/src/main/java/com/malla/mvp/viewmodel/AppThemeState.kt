package com.malla.mvp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.malla.mvp.network.ConnectivityMonitor
import com.malla.mvp.ui.theme.MallaColorScheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AppThemeState private constructor(private val prefs: SharedPreferences?) {

    companion object {
        private const val TAG = "AppThemeState"
        private const val KEY_SCHEME_NAME = "scheme_name"

        fun create(context: Context): AppThemeState {
            val prefs = try {
                context.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                Log.e(TAG, "[THEME:ERR] Error accediendo a SharedPreferences", e)
                null
            }
            return AppThemeState(prefs)
        }

        fun createFallback(): AppThemeState = AppThemeState(null)
        val availableSchemes = MallaColorScheme.ALL
    }

    private val _userSelectedScheme = MutableStateFlow(loadScheme())
    val userSelectedScheme: StateFlow<MallaColorScheme> = _userSelectedScheme

    private val _effectiveScheme = MutableStateFlow(MallaColorScheme.MALLA_DARK)
    val currentTheme: StateFlow<MallaColorScheme> = _effectiveScheme

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        scope.launch {
            combine(_userSelectedScheme, ConnectivityMonitor.isOnline) { userScheme, isOnline ->
                if (isOnline) userScheme else MallaColorScheme.OLED_PURE
            }.collect { scheme ->
                try {
                    _effectiveScheme.value = scheme
                    Log.d(TAG, "[THEME] Tema cambiado a: ${scheme.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "[THEME:ERR] Error al aplicar tema ${scheme.name}", e)
                    _effectiveScheme.value = MallaColorScheme.MALLA_DARK // Fallback seguro
                }
            }
        }
    }

    fun selectScheme(scheme: MallaColorScheme) {
        try {
            Log.d(TAG, "[THEME] Usuario seleccionó tema: ${scheme.name}")
            _userSelectedScheme.value = scheme
            saveScheme(scheme)
        } catch (e: Exception) {
            Log.e(TAG, "[THEME:ERR] Error al seleccionar tema ${scheme.name}", e)
        }
    }

    private fun loadScheme(): MallaColorScheme {
        return try {
            val name = prefs?.getString(KEY_SCHEME_NAME, MallaColorScheme.MALLA_DARK.name)
                ?: MallaColorScheme.MALLA_DARK.name
            MallaColorScheme.ALL.find { it.name == name } ?: MallaColorScheme.MALLA_DARK
        } catch (e: Exception) {
            Log.e(TAG, "[THEME:ERR] Error cargando tema, usando default", e)
            MallaColorScheme.MALLA_DARK
        }
    }

    private fun saveScheme(scheme: MallaColorScheme) {
        try {
            prefs?.edit()?.putString(KEY_SCHEME_NAME, scheme.name)?.apply()
            Log.d(TAG, "[THEME] Tema guardado: ${scheme.name}")
        } catch (e: Exception) {
            Log.e(TAG, "[THEME:ERR] Error guardando tema ${scheme.name}", e)
        }
    }
}
