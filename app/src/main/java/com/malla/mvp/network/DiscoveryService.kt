package com.malla.mvp.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.malla.mvp.identity.IdentityManager
import java.net.InetAddress

object DiscoveryService {
    private const val TAG = "DiscoveryService"
    var onPeerResolved: ((String) -> Unit)? = null
    private const val SERVICE_TYPE = "_malla._tcp"
    private var nsdManager: NsdManager? = null
    private var registeredService: NsdServiceInfo? = null

    fun start(context: Context) {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        // Registrar nuestro servicio
        registerService(context)
        // Descubrir otros servicios
        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        Log.d(TAG, "NSD iniciado (registro + descubrimiento)")
    }

    fun stop() {
        nsdManager?.unregisterService(registrationListener)
        nsdManager?.stopServiceDiscovery(discoveryListener)
        Log.d(TAG, "NSD detenido")
    }

    private fun registerService(context: Context) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "MALLA_${IdentityManager.deviceId}"
            serviceType = SERVICE_TYPE
            port = NetworkService.DEFAULT_PORT
            // La IP se asigna automáticamente al registrarse
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(TAG, "Registro NSD fallido: $errorCode")
        }
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            Log.d(TAG, "Servicio NSD registrado: ${serviceInfo?.serviceName}")
        }
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {}
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "NSD encontrado: ${serviceInfo.serviceName}")
            // Resolver el servicio para obtener la IP y el puerto
            nsdManager?.resolveService(serviceInfo, resolveListener)
        }
        override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        override fun onDiscoveryStopped(regType: String) {}
        override fun onStartDiscoveryFailed(regType: String, errorCode: Int) {
            Log.e(TAG, "Fallo al iniciar descubrimiento NSD: $errorCode")
        }
        override fun onStopDiscoveryFailed(regType: String, errorCode: Int) {}
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(TAG, "Resolución NSD fallida: $errorCode")
        }
        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            serviceInfo?.let {
                val host = it.host?.hostAddress ?: return
                val port = it.port
                Log.d(TAG, "Resuelto: $host:$port (${it.serviceName})")
                onPeerResolved?.invoke("$host:$port")
                // Por ahora, solo logueamos
            }
        }
    }
}
