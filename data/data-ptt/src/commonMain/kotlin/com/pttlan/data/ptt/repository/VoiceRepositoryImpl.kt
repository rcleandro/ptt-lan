package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.AudioRecorder
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.data.ptt.util.LocalFileCache
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.VoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okio.BufferedSink
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import kotlin.time.Clock

class VoiceRepositoryImpl(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val webSocketClient: PttWebSocketClient,
    private val database: PttDatabase,
    private val localFileCache: LocalFileCache,
) : VoiceRepository {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var transmissionJob: Job? = null
    private var receptionJob: Job? = null

    private var currentSpeakerId: String? = null
    private var currentChannelId: String? = null
    private var currentMessageStartMs: Long = 0
    private var currentFileSink: BufferedSink? = null
    private var currentFilePath: String? = null

    init {
        webSocketClient.controlMessages
            .onEach { msg ->
                if (msg is ControlMessage.SpeakerChanged) {
                    if (msg.isSpeaking) {
                        currentSpeakerId = msg.userId
                        currentChannelId = msg.channelId
                        currentMessageStartMs = Clock.System.now().toEpochMilliseconds()
                        val fileName = "${msg.channelId}_$currentMessageStartMs.pcm"
                        val path = "${localFileCache.getCacheDir()}/$fileName".toPath()
                        currentFilePath = path.toString()
                        try {
                            currentFileSink = FileSystem.SYSTEM.sink(path).buffer()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        currentFileSink?.close()
                        currentFileSink = null
                        if (currentSpeakerId == msg.userId && currentFilePath != null && currentChannelId != null) {
                            val duration = Clock.System.now().toEpochMilliseconds() - currentMessageStartMs
                            val id = "${msg.channelId}_$currentMessageStartMs"
                            database.voiceMessageQueries.insert(
                                id = id,
                                channelId = msg.channelId,
                                senderNickname = msg.userId, // using userId as fallback for now
                                filePath = currentFilePath!!,
                                durationMs = duration,
                                recordedAt = currentMessageStartMs,
                            )
                            // Enforce FIFO limit of 50
                            val count = database.voiceMessageQueries.countByChannel(msg.channelId).executeAsOne()
                            if (count > 50) {
                                val toDelete = count - 50
                                // Note: We need to make sure deleteOldestByChannel limits properly
                                database.voiceMessageQueries.deleteOldestByChannel(msg.channelId, toDelete)
                            }
                        }
                        currentSpeakerId = null
                        currentFilePath = null
                        currentChannelId = null
                    }
                }
            }.launchIn(scope)

        receptionJob =
            webSocketClient.audioChunks
                .onEach { (envelope, chunk) ->
                    audioPlayer.play(chunk)
                    try {
                        currentFileSink?.write(chunk)
                        currentFileSink?.flush()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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

        transmissionJob =
            scope.launch {
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

    override fun getRecentMessages(channelId: String): Flow<List<VoiceMessage>> =
        database.voiceMessageQueries
            .getRecentMessagesByChannel(channelId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    VoiceMessage(
                        id = it.id,
                        channelId = it.channelId,
                        senderNickname = it.senderNickname,
                        filePath = it.filePath,
                        durationMs = it.durationMs,
                        recordedAt = it.recordedAt,
                    )
                }
            }

    override suspend fun playMessage(message: VoiceMessage) {
        try {
            val path = message.filePath.toPath()
            val source = FileSystem.SYSTEM.source(path).buffer()
            val data = source.readByteArray()
            source.close()
            // Using existing player
            audioPlayer.play(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun stopPlayingMessage() {
        audioPlayer.stop()
    }
}
