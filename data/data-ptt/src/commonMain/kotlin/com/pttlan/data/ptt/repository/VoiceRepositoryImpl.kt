package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.pttlan.core.audio.AudioCodec
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.AudioRecorder
import com.pttlan.core.common.crypto.AudioCrypto
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.protocol.AudioCodecType
import com.pttlan.core.network.protocol.AudioEnvelope
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.BufferedSink
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class VoiceRepositoryImpl(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val webSocketClient: PttWebSocketClient,
    private val database: PttDatabase,
    private val pcmCodec: AudioCodec,
    private val opusCodec: AudioCodec,
    private val settings: Settings,
    private val storageInfoProvider: StorageInfoProvider,
) : VoiceRepository {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var transmissionJob: Job? = null
    private var receptionJob: Job? = null

    private var playbackJob: Job? = null
    private var isPlaybackPaused = false
    private var currentPlaybackMessageId: String? = null

    private var currentSpeakerId: String? = null
    private var currentSpeakerNickname: String? = null
    private var currentChannelId: String? = null
    private var currentMessageStartMs: Long = 0
    private var currentFileSink: BufferedSink? = null
    private var currentFilePath: String? = null
    private var currentAudioCrypto: AudioCrypto? = null

    init {
        webSocketClient.controlMessages
            .onEach { msg ->
                if (msg is ControlMessage.SpeakerChanged) {
                    if (msg.isSpeaking) {
                        currentSpeakerId = msg.userId
                        currentSpeakerNickname = msg.nickname
                        currentChannelId = msg.channelId
                        currentMessageStartMs = Clock.System.now().toEpochMilliseconds()
                        val allowCache = settings.getBoolean("allow_cache", false)
                        if (allowCache) {
                            val cacheLocation = settings.getString("cache_location", "Interno")
                            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation)
                            if (dirPath != null) {
                                val fileName = "${msg.channelId}_$currentMessageStartMs.pcm"
                                val path = "$dirPath/$fileName".toPath()
                                currentFilePath = path.toString()
                                try {
                                    currentFileSink = FileSystem.SYSTEM.sink(path).buffer()
                                    currentAudioCrypto = AudioCrypto()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else {
                        currentFileSink?.close()
                        currentFileSink = null
                        currentAudioCrypto = null
                        if (currentSpeakerId == msg.userId && currentChannelId != null) {
                            if (currentFilePath != null) {
                                val path = currentFilePath!!.toPath()
                                val size =
                                    try {
                                        FileSystem.SYSTEM.metadataOrNull(path)?.size ?: 0L
                                    } catch (e: Exception) {
                                        0L
                                    }
                                if (size == 0L) {
                                    try {
                                        FileSystem.SYSTEM.delete(path)
                                    } catch (e: Exception) {
                                    }
                                } else {
                                    val duration = Clock.System.now().toEpochMilliseconds() - currentMessageStartMs
                                    val id = "${msg.channelId}_$currentMessageStartMs"
                                    database.voiceMessageQueries.insert(
                                        id = id,
                                        channelId = msg.channelId,
                                        senderNickname = currentSpeakerNickname ?: msg.userId,
                                        filePath = currentFilePath!!,
                                        durationMs = duration,
                                        recordedAt = currentMessageStartMs,
                                    )
                                    val count = database.voiceMessageQueries.countByChannel(msg.channelId).executeAsOne()
                                    if (count > 50) {
                                        val toDelete = count - 50
                                        database.voiceMessageQueries.deleteOldestByChannel(msg.channelId, toDelete)
                                    }
                                    manageCache()
                                }
                            }
                        }
                        currentSpeakerId = null
                        currentSpeakerNickname = null
                        currentFilePath = null
                        currentChannelId = null
                    }
                }
            }.launchIn(scope)

        receptionJob =
            webSocketClient.audioChunks
                .onEach { (envelope, chunk) ->
                    val decoded =
                        try {
                            if (envelope?.codec == AudioCodecType.OPUS) {
                                opusCodec.decode(chunk)
                            } else {
                                pcmCodec.decode(chunk)
                            }
                        } catch (_: Exception) {
                            chunk
                        }
                    audioPlayer.play(decoded)
                    try {
                        if (currentFileSink != null && currentAudioCrypto != null) {
                            val encrypted = currentAudioCrypto!!.process(decoded)
                            currentFileSink?.write(encrypted)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.launchIn(scope)
    }

    private fun manageCache() {
        try {
            val maxCacheSizeMb = settings.getInt("max_cache_size_mb", 500)
            val limitBytes = maxCacheSizeMb * 1024L * 1024L
            val cacheLocation = settings.getString("cache_location", "Interno")
            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation) ?: return

            val dir = dirPath.toPath()
            val files = FileSystem.SYSTEM.list(dir).filter { it.name.endsWith(".pcm") }
            var totalSize = files.sumOf { FileSystem.SYSTEM.metadata(it).size ?: 0L }

            if (totalSize > limitBytes) {
                val sortedFiles = files.sortedBy { FileSystem.SYSTEM.metadata(it).lastModifiedAtMillis ?: 0L }
                for (file in sortedFiles) {
                    if (totalSize <= limitBytes) break
                    val size = FileSystem.SYSTEM.metadata(file).size ?: 0L
                    FileSystem.SYSTEM.delete(file)
                    database.voiceMessageQueries.deleteByFilePath(file.toString())
                    totalSize -= size
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        val useOpus = settings.getBoolean("use_opus", false)
        val codecType =
            if (useOpus) {
                AudioCodecType.OPUS
            } else {
                AudioCodecType.PCM16
            }
        val codec = if (useOpus) opusCodec else pcmCodec
        var sequenceNumber = 0

        transmissionJob =
            scope.launch {
                val audioStream = audioRecorder.startCapture()
                audioStream.collect { chunk ->
                    val encoded =
                        try {
                            codec.encode(chunk)
                        } catch (_: Exception) {
                            chunk
                        }
                    val envelope =
                        AudioEnvelope(
                            channelId = channelId,
                            senderId = userId,
                            sequenceNumber = sequenceNumber++,
                            codec = codecType,
                            timestampMs = Clock.System.now().toEpochMilliseconds(),
                        )
                    webSocketClient.sendAudioChunk(envelope, encoded)

                    try {
                        if (currentFileSink != null && currentAudioCrypto != null) {
                            val encrypted = currentAudioCrypto!!.process(chunk)
                            currentFileSink?.write(encrypted)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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

    override fun getAllMessages(): Flow<List<VoiceMessage>> =
        database.voiceMessageQueries
            .getAllMessages()
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
        stopPlayingMessage()
        currentPlaybackMessageId = message.id
        isPlaybackPaused = false

        playbackJob =
            scope.launch {
                try {
                    val path = message.filePath.toPath()
                    val source = FileSystem.SYSTEM.source(path).buffer()
                    val crypto = AudioCrypto()
                    val buffer = ByteArray(4096)

                    while (isActive) {
                        if (isPlaybackPaused) {
                            delay(100.milliseconds)
                            continue
                        }
                        val read = source.read(buffer)
                        if (read == -1) break
                        val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                        val decrypted = crypto.process(chunk)
                        audioPlayer.play(decrypted)
                    }
                    source.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    audioPlayer.stop()
                    currentPlaybackMessageId = null
                    isPlaybackPaused = false
                }
            }
        playbackJob?.join()
    }

    override suspend fun pausePlayingMessage() {
        if (currentPlaybackMessageId != null) {
            isPlaybackPaused = true
        }
    }

    override suspend fun resumePlayingMessage() {
        if (currentPlaybackMessageId != null) {
            isPlaybackPaused = false
        }
    }

    override suspend fun stopPlayingMessage() {
        playbackJob?.cancel()
        playbackJob = null
        isPlaybackPaused = false
        currentPlaybackMessageId = null
        audioPlayer.stop()
    }

    override suspend fun clearAllMessages() {
        stopPlayingMessage()
        database.voiceMessageQueries.deleteAllMessages()

        try {
            val cacheLocation = settings.getString("cache_location", "Interno")
            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation) ?: return
            val dir = dirPath.toPath()
            val files = FileSystem.SYSTEM.list(dir).filter { it.name.endsWith(".pcm") }
            for (file in files) {
                FileSystem.SYSTEM.delete(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteMessage(message: VoiceMessage) {
        if (currentPlaybackMessageId == message.id) {
            stopPlayingMessage()
        }
        database.voiceMessageQueries.deleteById(message.id)
        try {
            val path = message.filePath.toPath()
            FileSystem.SYSTEM.delete(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteChannelMessages(channelId: String) {
        val messages = database.voiceMessageQueries.getRecentMessagesByChannel(channelId).executeAsList()
        messages.forEach { message ->
            if (currentPlaybackMessageId == message.id) {
                stopPlayingMessage()
            }
            try {
                val path = message.filePath.toPath()
                FileSystem.SYSTEM.delete(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        database.voiceMessageQueries.deleteAllByChannel(channelId)
    }
}
