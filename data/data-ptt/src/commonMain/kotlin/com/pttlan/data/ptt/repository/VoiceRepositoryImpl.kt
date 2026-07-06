package com.pttlan.data.ptt.repository

import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.AudioRecorder
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.domain.ptt.repository.VoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class VoiceRepositoryImpl(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val webSocketClient: PttWebSocketClient
) : VoiceRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var transmissionJob: Job? = null
    private var receptionJob: Job? = null

    init {
        // Start listening for incoming audio chunks from network
        receptionJob = webSocketClient.audioChunks
            .onEach { (envelope, chunk) ->
                // For MVP we just play raw chunks as they come
                audioPlayer.play(chunk)
            }
            .launchIn(scope)
    }

    override suspend fun requestFloor(channelId: String, userId: String) {
        webSocketClient.sendControlMessage(com.pttlan.core.network.protocol.ControlMessage.StartSpeaking(channelId, userId))
    }

    override suspend fun releaseFloor(channelId: String, userId: String) {
        webSocketClient.sendControlMessage(com.pttlan.core.network.protocol.ControlMessage.StopSpeaking(channelId, userId))
    }

    override suspend fun startTransmitting(channelId: String, userId: String) {
        transmissionJob?.cancel()
        
        transmissionJob = scope.launch {
            val audioStream = audioRecorder.startCapture()
            audioStream.collect { chunk ->
                webSocketClient.sendAudioChunk(chunk)
            }
        }
    }

    override suspend fun stopTransmitting() {
        transmissionJob?.cancel()
        transmissionJob = null
        audioRecorder.stopCapture()
    }
}
