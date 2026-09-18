package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import com.pttlan.core.audio.AudioCodec
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.AudioRecorder
import com.pttlan.core.common.crypto.AudioCrypto
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
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
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : VoiceRepository {
    private val logger = Logger.withTag("audio")
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
                        val allowCache = settings.getBoolean(SettingsKeys.ALLOW_CACHE, SettingsDefaults.ALLOW_CACHE)
                        if (allowCache) {
                            val cacheLocation = settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION)
                            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation)
                            if (dirPath != null) {
                                val fileName = "${msg.channelId}_$currentMessageStartMs.pcm"
                                val path = "$dirPath/$fileName".toPath()
                                currentFilePath = path.toString()
                                try {
                                    currentFileSink = fileSystem.sink(path).buffer()
                                    currentAudioCrypto = AudioCrypto()
                                } catch (e: Exception) {
                                    logger.w(e) { "Failed to open the recording file" }
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
                                        fileSystem.metadataOrNull(path)?.size ?: 0L
                                    } catch (_: Exception) {
                                        0L
                                    }
                                if (size == 0L) {
                                    try {
                                        fileSystem.delete(path)
                                    } catch (_: Exception) {
                                        // Ignore
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
                                    if (count > MAX_MESSAGES_PER_CHANNEL) {
                                        purgeOldestMessages(
                                            queries = database.voiceMessageQueries,
                                            fileSystem = fileSystem,
                                            channelId = msg.channelId,
                                            toDelete = count - MAX_MESSAGES_PER_CHANNEL,
                                        )
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
                    audioPlayer.play(
                        chunk = decoded,
                        sequenceNumber = envelope?.sequenceNumber ?: 0,
                        timestampMs = envelope?.timestampMs ?: 0L,
                    )
                    try {
                        if (currentFileSink != null && currentAudioCrypto != null) {
                            val encrypted = currentAudioCrypto!!.process(decoded)
                            currentFileSink?.write(encrypted)
                        }
                    } catch (e: Exception) {
                        logger.w(e) { "Failed to store incoming audio" }
                    }
                }.launchIn(scope)
    }

    private fun manageCache() {
        try {
            val maxCacheSizeMb = settings.getInt(SettingsKeys.MAX_CACHE_SIZE_MB, SettingsDefaults.MAX_CACHE_SIZE_MB)
            val limitBytes = maxCacheSizeMb * 1024L * 1024L
            val cacheLocation = settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION)
            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation) ?: return

            val dir = dirPath.toPath()
            val files = fileSystem.list(dir).filter { it.name.endsWith(".pcm") }
            var totalSize = files.sumOf { fileSystem.metadata(it).size ?: 0L }

            if (totalSize > limitBytes) {
                val sortedFiles = files.sortedBy { fileSystem.metadata(it).lastModifiedAtMillis ?: 0L }
                for (file in sortedFiles) {
                    if (totalSize <= limitBytes) break
                    val size = fileSystem.metadata(file).size ?: 0L
                    fileSystem.delete(file)
                    database.voiceMessageQueries.deleteByFilePath(file.toString())
                    totalSize -= size
                }
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to trim the history by size" }
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

        val useOpus = settings.getBoolean(SettingsKeys.USE_OPUS, SettingsDefaults.USE_OPUS)
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
                audioStream
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
                            logger.w(e) { "Failed to store outgoing audio" }
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
                    val source = fileSystem.source(path).buffer()
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
                    logger.w(e) { "Failed to play a message from the history" }
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
            val cacheLocation = settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION)
            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation) ?: return
            val dir = dirPath.toPath()
            val files = fileSystem.list(dir).filter { it.name.endsWith(".pcm") }
            for (file in files) {
                fileSystem.delete(file)
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to delete audio files" }
        }
    }

    override suspend fun deleteMessage(message: VoiceMessage) {
        if (currentPlaybackMessageId == message.id) {
            stopPlayingMessage()
        }
        database.voiceMessageQueries.deleteById(message.id)
        try {
            val path = message.filePath.toPath()
            fileSystem.delete(path)
        } catch (e: Exception) {
            logger.w(e) { "Failed to delete the message file" }
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
                fileSystem.delete(path)
            } catch (e: Exception) {
                logger.w(e) { "Failed to delete purged files" }
            }
        }
        database.voiceMessageQueries.deleteAllByChannel(channelId)
    }
}
