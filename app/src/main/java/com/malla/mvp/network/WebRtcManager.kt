package com.malla.mvp.network

import android.content.Context
import android.media.AudioManager
import com.malla.mvp.core.engine.LogBuffer
import com.malla.mvp.di.Injector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.*

class WebRtcManager(private val context: Context) {
    companion object {
        private const val TAG = "WebRtcManager"
    }

    sealed class CallState {
        object Idle : CallState()
        object Outgoing : CallState()
        data class Incoming(val contactId: String, val callType: String) : CallState()
        object Connected : CallState()
        object Ended : CallState()
    }

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    var remoteVideoView: SurfaceViewRenderer? = null
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var currentContactId: String? = null
    private var currentCallType: String = "voice"

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setFieldTrials("")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val eglBase = EglBase.create()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        CoroutineScope(Dispatchers.IO).launch {
            NetworkService.messages.collect { msg ->
                if (msg.type == 5) {
                    handleSignal(msg.content, msg.senderId)
                }
            }
        }
    }

    fun startVoiceCall(contactId: String) {
        if (_callState.value != CallState.Idle) return
        currentContactId = contactId
        currentCallType = "voice"
        _callState.value = CallState.Outgoing
        LogBuffer.add(TAG, "Iniciando llamada de voz con $contactId")
        createPeerConnection()
        createOffer()
    }

    fun startVideoCall(contactId: String) {
        if (_callState.value != CallState.Idle) return
        currentContactId = contactId
        currentCallType = "video"
        _callState.value = CallState.Outgoing
        LogBuffer.add(TAG, "Iniciando videollamada con $contactId")
        createPeerConnection()
        createOffer()
    }

    fun acceptCall(contactId: String, callType: String) {
        if (_callState.value !is CallState.Incoming) return
        currentContactId = contactId
        currentCallType = callType
        _callState.value = CallState.Connected
        LogBuffer.add(TAG, "Llamada aceptada: $callType de $contactId")
        createPeerConnection()
    }

    fun endCall() {
        LogBuffer.add(TAG, "Finalizando llamada")
        _callState.value = CallState.Ended
        cleanupCall()
        _callState.value = CallState.Idle
        Injector.messageBridge.sendWebRtcSignal(currentContactId ?: return, "call_end:")
    }

    fun isInCall(): Boolean = _callState.value != CallState.Idle && _callState.value != CallState.Ended
    fun isVideoCall(): Boolean = currentCallType == "video"

    fun setSpeakerphoneOn(on: Boolean) { audioManager.isSpeakerphoneOn = on }
    fun setMicrophoneMute(mute: Boolean) { audioManager.setMicrophoneMute(mute) }

    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )).apply {
        iceTransportsType = PeerConnection.IceTransportsType.ALL
    }
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { sendIceCandidate(it) }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    _callState.value = CallState.Connected
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                receiver?.track()?.let { track ->
                    if (track is VideoTrack) {
                        remoteVideoView?.let { track.addSink(it) }
                    }
                }
            }
        }
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio0", audioSource)
        peerConnection?.addTrack(localAudioTrack)
        if (currentCallType == "video") {
            videoCapturer = createCameraCapturer()
            videoSource = peerConnectionFactory?.createVideoSource(false)
            videoCapturer?.initialize(SurfaceTextureHelper.create("CaptureThread", null), context, videoSource?.capturerObserver)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("video0", videoSource)
            peerConnection?.addTrack(localVideoTrack)
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (currentCallType == "video") "true" else "false"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let { peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { sendSdp(sdp.description, sdp.type.canonicalForm()) }
                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, it) }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun sendSdp(description: String, type: String) {
        Injector.messageBridge.sendWebRtcSignal(currentContactId ?: return, "sdp:$type:$description")
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val signal = "ice:${candidate.sdp}:${candidate.sdpMLineIndex}:${candidate.sdpMid}"
        Injector.messageBridge.sendWebRtcSignal(currentContactId ?: return, signal)
    }

    private fun handleSignal(content: String, senderId: String) {
        val parts = content.split(":", limit = 3)
        when (parts[0]) {
            "call_start" -> {
                val callType = parts.getOrElse(1) { "voice" }
                if (_callState.value == CallState.Idle) {
                    currentContactId = senderId
                    currentCallType = callType
                    _callState.value = CallState.Incoming(senderId, callType)
                }
            }
            "call_accept" -> { if (_callState.value == CallState.Outgoing) _callState.value = CallState.Connected }
            "call_end" -> { if (_callState.value !is CallState.Idle) endCall() }
            "sdp" -> {
                if (parts.size == 3) {
                    val sdp = SessionDescription(if (parts[1] == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER, parts[2])
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() { if (parts[1] == "offer") createAnswer() }
                        override fun onSetFailure(p0: String?) {}
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
            }
            "ice" -> {
                if (parts.size == 3) {
                    val iceParts = parts[2].split(":")
                    if (iceParts.size == 3) {
                        peerConnection?.addIceCandidate(IceCandidate(iceParts[2], iceParts[1].toIntOrNull() ?: return, iceParts[0]))
                    }
                }
            }
        }
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (currentCallType == "video") "true" else "false"))
        }
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let { peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { sendSdp(sdp.description, sdp.type.canonicalForm()) }
                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, it) }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera1Enumerator(false)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    private fun cleanupCall() {
        try { localAudioTrack?.setEnabled(false) } catch (_: Exception) {}
        try { localVideoTrack?.setEnabled(false) } catch (_: Exception) {}
        try { peerConnection?.close() } catch (_: Exception) {}
        peerConnection = null
        audioSource = null
        localAudioTrack = null
        videoCapturer?.dispose()
        videoCapturer = null
        videoSource = null
        localVideoTrack = null
    }
}
