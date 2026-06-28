package com.malla.mvp.core.transport

import android.content.Context
import android.hardware.camera2.*
import android.os.Handler
import android.os.Looper
import com.malla.mvp.core.engine.LogBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class FlashlightTransport(private val context: Context) {
    companion object {
        private const val TAG = "FlashlightTransport"
        private const val BIT_DURATION_MS = 80L
    }

    private val _incomingMessages = MutableSharedFlow<String>(replay = 0)
    val incomingMessages: SharedFlow<String> = _incomingMessages
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var isTransmitting = false
    private var isReceiving = false
    private var transmissionProgress = MutableStateFlow(0f)

    fun start() {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager?.cameraIdList?.firstOrNull()
        LogBuffer.add(TAG, "FlashlightTransport iniciado")
    }

    fun startTransmitting(message: String) {
        if (isTransmitting || cameraId == null) return
        isTransmitting = true
        scope.launch {
            try {
                val bits = LightEncoder.encode(message)
                LogBuffer.add(TAG, "Transmitiendo ${bits.size} bits: ${message.take(30)}...")
                cameraManager?.setTorchMode(cameraId!!, false)
                delay(500)
                for ((index, bit) in bits.withIndex()) {
                    cameraManager?.setTorchMode(cameraId!!, bit == 1)
                    delay(BIT_DURATION_MS)
                    transmissionProgress.value = (index + 1).toFloat() / bits.size
                }
                cameraManager?.setTorchMode(cameraId!!, false)
                LogBuffer.add(TAG, "Transmisión completada")
            } catch (e: Exception) {
                LogBuffer.add(TAG, "Error transmitiendo: ${e.message}")
            } finally {
                isTransmitting = false
                transmissionProgress.value = 0f
            }
        }
    }

    fun stopTransmitting() {
        isTransmitting = false
        try { cameraManager?.setTorchMode(cameraId!!, false) } catch (_: Exception) {}
    }

    fun getTransmissionProgress(): StateFlow<Float> = transmissionProgress

    fun startReceiving() {
        if (isReceiving || cameraId == null) return
        isReceiving = true
        scope.launch {
            try {
                val receivedBits = mutableListOf<Int>()
                var lastBright = false
                var lastChangeTime = System.currentTimeMillis()
                val handler = Handler(Looper.getMainLooper())
                cameraManager?.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        camera.createCaptureSession(emptyList(), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                    addTarget(android.view.Surface(null))
                                }
                                session.setRepeatingRequest(request.build(), object : CameraCaptureSession.CaptureCallback() {
                                    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                        val aeState = result[CaptureResult.CONTROL_AE_STATE]
                                        val bright = aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                                        if (bright != lastBright) {
                                            val now = System.currentTimeMillis()
                                            val bit = if (bright) 1 else 0
                                            receivedBits.add(bit)
                                            lastChangeTime = now
                                            lastBright = bright
                                            if (receivedBits.size % 100 == 0) {
                                                val text = LightEncoder.decode(receivedBits.toList())
                                                if (text != null) {
                                                    _incomingMessages.tryEmit(text)
                                                    receivedBits.clear()
                                                    LogBuffer.add(TAG, "Mensaje recibido por Faro: $text")
                                                }
                                            }
                                        }
                                    }
                                }, handler)
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        }, handler)
                    }
                    override fun onDisconnected(camera: CameraDevice) { camera.close() }
                    override fun onError(camera: CameraDevice, error: Int) { camera.close() }
                }, handler)
            } catch (e: Exception) {
                LogBuffer.add(TAG, "Error recibiendo: ${e.message}")
            }
        }
    }

    fun stopReceiving() {
        isReceiving = false
    }
}
