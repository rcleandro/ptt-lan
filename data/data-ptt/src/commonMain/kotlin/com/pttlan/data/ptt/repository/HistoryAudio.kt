package com.pttlan.data.ptt.repository

import com.pttlan.core.audio.OpusAudioCodec
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/** Mono 16 bit PCM at 48 kHz, what the app captures, plays and used to record. */
internal const val PCM_BYTES_PER_SECOND = 48_000L * 2
private const val MS_PER_SECOND = 1000L

/** 20 ms of PCM: the Opus frame the history records and replays. */
internal const val OPUS_FRAME_BYTES = 1920
private const val OPUS_FRAME_MS = 20L

/** Chunk read from a PCM recording at a time. */
private const val PCM_READ_BYTES = 4096

/**
 * Extension of a history recording in Opus (31.4): a sequence of 20 ms Opus frames, each preceded by its size in
 * two bytes. About a tenth of the PCM it replaced (`.pcm`, 5.8 MB a minute), and still readable frame by frame,
 * which is what seeking and the replay speed need.
 */
internal const val OPUS_EXTENSION = "opf"
internal const val PCM_EXTENSION = "pcm"

internal fun pcmMs(bytes: Long) = bytes * MS_PER_SECOND / PCM_BYTES_PER_SECOND

internal fun isRecording(path: Path) = path.name.endsWith(".$OPUS_EXTENSION") || path.name.endsWith(".$PCM_EXTENSION")

/** Encodes PCM into the Opus frames of a recording, whatever the size of the chunks it gets. */
internal class OpusFrameWriter(
    private val sink: BufferedSink,
) {
    private val codec = OpusAudioCodec()
    private var pending = ByteArray(0)

    fun write(pcm: ByteArray) {
        pending += pcm
        while (pending.size >= OPUS_FRAME_BYTES) {
            writeFrame(pending.copyOfRange(0, OPUS_FRAME_BYTES))
            pending = pending.copyOfRange(OPUS_FRAME_BYTES, pending.size)
        }
    }

    /**
     * A 20 ms frame that is already Opus (what went out or came in on the wire), stored as is: encoding its PCM
     * again cost about a fifth of the CPU of whoever was speaking.
     */
    fun writeEncoded(frame: ByteArray) {
        sink.writeShort(frame.size)
        sink.write(frame)
    }

    /** The last partial frame, completed with silence, so the end of the speech is kept. */
    fun close() {
        if (pending.isNotEmpty()) writeFrame(pending.copyOf(OPUS_FRAME_BYTES))
        pending = ByteArray(0)
        sink.close()
    }

    private fun writeFrame(frame: ByteArray) {
        val encoded = codec.encode(frame)
        if (encoded.isEmpty()) return
        sink.writeShort(encoded.size)
        sink.write(encoded)
    }
}

/** A recording read as PCM, whatever its format: the replay, the seek and the export only see this. */
internal interface RecordingSource {
    val totalMs: Long
    val positionMs: Long

    /** The next PCM, or null at the end. */
    fun read(): ByteArray?

    fun seek(targetMs: Long)

    fun close()
}

internal fun openRecording(
    fileSystem: FileSystem,
    path: Path,
    fallbackDurationMs: Long,
): RecordingSource =
    if (path.name.endsWith(".$OPUS_EXTENSION")) {
        OpusRecordingSource(fileSystem, path)
    } else {
        PcmRecordingSource(fileSystem, path, fallbackDurationMs)
    }

/** The whole recording as PCM, for the `.wav` export. */
internal fun readAllPcm(
    fileSystem: FileSystem,
    path: Path,
): ByteArray {
    val recording = openRecording(fileSystem, path, fallbackDurationMs = 0)
    val pcm = Buffer()
    try {
        while (true) pcm.write(recording.read() ?: break)
    } finally {
        recording.close()
    }
    return pcm.readByteArray()
}

/** The old format: raw PCM, playing until the history is cleared. */
private class PcmRecordingSource(
    private val fileSystem: FileSystem,
    private val path: Path,
    fallbackDurationMs: Long,
) : RecordingSource {
    private val sizeBytes = fileSystem.metadataOrNull(path)?.size

    /** The file is the honest total: a message's `durationMs` is wall clock of the talk spurt. */
    override val totalMs = sizeBytes?.let(::pcmMs) ?: fallbackDurationMs

    private var source = fileSystem.source(path).buffer()
    private val buffer = ByteArray(PCM_READ_BYTES)

    // From the bytes read, not a sum of per-chunk milliseconds, whose rounding would never reach the end
    private var readBytes = 0L
    override val positionMs get() = pcmMs(readBytes).coerceAtMost(totalMs)

    override fun read(): ByteArray? {
        val read = source.read(buffer)
        if (read == -1) return null
        readBytes += read
        // A copy: the player keeps the array in its queue, and the next read would overwrite it
        return buffer.copyOf(read)
    }

    /** Reads on from [targetMs], on a whole 16 bit sample. */
    override fun seek(targetMs: Long) {
        source.close()
        source = fileSystem.source(path).buffer()
        readBytes = (targetMs * PCM_BYTES_PER_SECOND / MS_PER_SECOND).coerceIn(0L, sizeBytes ?: 0L) and 1L.inv()
        source.skip(readBytes)
    }

    override fun close() = source.close()
}

/** A recording in Opus frames, decoded 20 ms at a time. */
private class OpusRecordingSource(
    private val fileSystem: FileSystem,
    private val path: Path,
) : RecordingSource {
    private val frameCount = countFrames()
    override val totalMs = frameCount * OPUS_FRAME_MS

    private var source = fileSystem.source(path).buffer()
    private val codec = OpusAudioCodec()
    private var framesRead = 0L
    override val positionMs get() = framesRead * OPUS_FRAME_MS

    override fun read(): ByteArray? {
        val frame = source.nextFrame() ?: return null
        framesRead++
        return codec.decode(frame)
    }

    /** Frames are 20 ms each: skip whole ones up to [targetMs]. */
    override fun seek(targetMs: Long) {
        source.close()
        source = fileSystem.source(path).buffer()
        framesRead = 0
        val target = (targetMs / OPUS_FRAME_MS).coerceIn(0L, frameCount)
        while (framesRead < target && source.skipFrame()) framesRead++
    }

    override fun close() = source.close()

    private fun countFrames(): Long =
        fileSystem.source(path).buffer().use { counting ->
            var frames = 0L
            while (counting.skipFrame()) frames++
            frames
        }
}

private fun BufferedSource.nextFrame(): ByteArray? = if (exhausted()) null else readByteArray(readShort().toLong())

private fun BufferedSource.skipFrame(): Boolean {
    if (exhausted()) return false
    skip(readShort().toLong())
    return true
}
