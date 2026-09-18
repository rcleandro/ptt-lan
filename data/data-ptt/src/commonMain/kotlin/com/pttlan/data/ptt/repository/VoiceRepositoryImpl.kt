package com.pttlan.data.ptt.repository

import co.touchlab.kermit.Logger
import com.pttlan.core.audio.AudioCodec
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.AudioRecorder
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.protocol.AudioCodecType
import com.pttlan.core.network.protocol.AudioEnvelope
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The live audio path: floor control, capture and playback of what arrives. Recording to the cache is
 * delegated to [HistoryRecorder], which owns that state on its own dispatcher (22.7).
 */
class VoiceRepositoryImpl(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val webSocketClient: PttWebSocketClient,
    private val pcmCodec: AudioCodec,
    private val opusCodec: AudioCodec,
    private val settings: Settings,
    private val recorder: HistoryRecorder,
) : VoiceRepository {
    private val logger = Logger.withTag("audio")
    private val scope = CoroutineScope(Dispatchers.Default)
    private var transmissionJob: Job? = null

    init {
        webSocketClient.controlMessages
            .onEach { message ->
                if (message is ControlMessage.SpeakerChanged) {
                    if (message.isSpeaking) {
                        recorder.onSpeakerStarted(message.channelId, message.userId, message.nickname)
                    } else {
                        recorder.onSpeakerStopped(message.userId)
                    }
                }
            }.launchIn(scope)

        webSocketClient.audioChunks
            .onEach { (envelope, chunk) ->
                val decoded =
                    try {
                        if (envelope?.codec == AudioCodecType.OPUS) {
                            opusCodec.decode(chunk)
                        } else {
                            pcmCodec.decode(chunk)
                        }
                    } catch (e: Exception) {
                        logger.w(e) { "Failed to decode incoming audio" }
                        chunk
                    }
                audioPlayer.play(
                    chunk = decoded,
                    sequenceNumber = envelope?.sequenceNumber ?: 0,
                    timestampMs = envelope?.timestampMs ?: 0L,
                )
                recorder.write(decoded)
            }.launchIn(scope)
    }

    override suspend fun requestFloor(
        channelId: String,
        userId: String,
    ) {
        webSocketClient.sendControlMessage(ControlMessage.StartSpeaking(channelId, userId))
    }

    override suspend fun releaseFloor(
        channelId: String,
        userId: String,
    ) {
        webSocketClient.sendControlMessage(ControlMessage.StopSpeaking(channelId, userId))
    }

    override suspend fun startTransmitting(
        channelId: String,
        userId: String,
    ) {
        transmissionJob?.cancel()

        val useOpus = settings.getBoolean(SettingsKeys.USE_OPUS, SettingsDefaults.USE_OPUS)
        val codecType = if (useOpus) AudioCodecType.OPUS else AudioCodecType.PCM16
        val codec = if (useOpus) opusCodec else pcmCodec
        var sequenceNumber = 0

        transmissionJob =
            scope.launch {
                audioRecorder
                    .startCapture()
                    .buffer(100, BufferOverflow.DROP_OLDEST)
                    .collect { chunk ->
                        val encoded =
                            try {
                                codec.encode(chunk)
                            } catch (e: Exception) {
                                logger.e(e) { "Failed to encode audio with $codecType" }
                                ByteArray(0)
                            }
                        if (encoded.isEmpty()) {
                            // Sending the raw chunk instead would label PCM as Opus and break every listener
                            logger.w { "Dropped a ${chunk.size} byte frame: the encoder returned nothing" }
                            return@collect
                        }

                        webSocketClient.sendAudioChunk(
                            AudioEnvelope(
                                channelId = channelId,
                                senderId = userId,
                                sequenceNumber = sequenceNumber++,
                                codec = codecType,
                                timestampMs = Clock.System.now().toEpochMilliseconds(),
                            ),
                            encoded,
                        )
                        recorder.write(chunk)
                    }
            }
    }

    override suspend fun stopTransmitting() {
        transmissionJob?.cancel()
        transmissionJob = null
        audioRecorder.stopCapture()
    }
}
