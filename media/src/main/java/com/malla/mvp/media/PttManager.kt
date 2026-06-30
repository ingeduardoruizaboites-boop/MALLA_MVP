package com.malla.mvp.media

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Gestor de Push-to-Talk (PTT) para comunicación de voz en tiempo real sobre la red mesh.
 *
 * Principios:
 * - Audio mono, 8kHz, 16-bit PCM.
 * - Chunks de 20ms (320 bytes) para baja latencia.
 * - Captura con AudioRecord, reproducción con AudioTrack.
 * - Los chunks se envían como mensajes de tipo "voice" a través de NetworkService.
 * - La reproducción se activa automáticamente al recibir un chunk.
 */
object PttManager {
    private const val TAG = "PttManager"

    private const val SAMPLE_RATE = 8000
    private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
    private const val CHUNK_MS = 20
    private const val CHUNK_SIZE = 320

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private var playingJob: Job? = null
    private var isPlaying = false

    private val playQueue = ConcurrentLinkedQueue<ByteArray>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callback para enviar chunks de voz
    var onVoiceChunk: ((ByteArray) -> Unit)? = null

    /**
     * Inicia la captura de audio del micrófono y envía chunks al callback.
     * Debe llamarse al presionar el botón PTT.
     */
    fun startTransmitting() {
        if (audioRecord != null) return
        recordingJob = scope.launch {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
                )
                audioRecord?.startRecording()
                val buffer = ByteArray(CHUNK_SIZE)

                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: -1
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        onVoiceChunk?.invoke(chunk)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en transmisión PTT: ${e.message}", e)
            } finally {
                stopTransmitting()
            }
        }
    }

    /**
     * Detiene la captura de audio y libera el micrófono.
     */
    fun stopTransmitting() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener AudioRecord: ${e.message}", e)
        }
        audioRecord = null
    }

    /**
     * Recibe un chunk de voz y lo encola para reproducción.
     * Se llama desde MessageBridge al recibir un mensaje tipo "voice".
     */
    fun enqueueForPlayback(chunk: ByteArray) {
        playQueue.add(chunk)
        if (!isPlaying) {
            startPlayback()
        }
    }

    private fun startPlayback() {
        isPlaying = true
        playingJob = scope.launch {
            try {
                audioTrack = AudioTrack(
                    android.media.AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_OUT,
                    AUDIO_FORMAT,
                    BUFFER_SIZE,
                    AudioTrack.MODE_STREAM
                )
                audioTrack?.play()

                while (isActive && isPlaying) {
                    val chunk = playQueue.poll()
                    if (chunk != null) {
                        audioTrack?.write(chunk, 0, chunk.size)
                    } else {
                        delay(5) // esperar más chunks
                        // Si la cola sigue vacía después de 200ms, detener
                        if (playQueue.isEmpty()) {
                            delay(200)
                            if (playQueue.isEmpty()) {
                                isPlaying = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en reproducción PTT: ${e.message}", e)
            } finally {
                stopPlayback()
            }
        }
    }

    private fun stopPlayback() {
        isPlaying = false
        playingJob?.cancel()
        playingJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener AudioTrack: ${e.message}", e)
        }
        audioTrack = null
    }

    /**
     * Libera todos los recursos de audio.
     */
    fun shutdown() {
        stopTransmitting()
        stopPlayback()
        playQueue.clear()
    }
}
