package com.malla.mvp.media

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class VoiceRecorder(private val context: Context) {
    companion object {
        private const val TAG = "VoiceRecorder"
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording = false
        private set

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    private var amplitudeJob: Job? = null

    fun startRecording(): File? {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permiso RECORD_AUDIO no concedido")
            return null
        }

        outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile?.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioChannels(1)
                prepare()
                start()
                Log.d(TAG, "Grabación AAC iniciada")
                isRecording = true
                startAmplitudeSampling()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar grabación: ${e.message}", e)
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            return null
        }
        return outputFile!!
    }

    private fun startAmplitudeSampling() {
        amplitudeJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isRecording) {
                try {
                    _amplitude.value = recorder?.maxAmplitude ?: 0
                } catch (_: Exception) {}
                delay(50)
            }
        }
    }

    fun stopRecording(): File? {
        amplitudeJob?.cancel()
        recorder?.apply {
            try {
                stop()
                Log.d(TAG, "Grabación detenida. Archivo: ${outputFile?.absolutePath}")
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener grabación: ${e.message}", e)
            }
        }
        recorder = null
        isRecording = false
        return outputFile
    }
}
