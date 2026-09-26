package com.pttlan.server

import com.pttlan.core.audio.OpusAudioCodec
import com.pttlan.core.common.DEFAULT_CHANNEL_ID
import com.pttlan.core.common.DEFAULT_SERVER_PORT
import com.pttlan.core.common.TooManyAttemptsException
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.createHttpClient
import com.pttlan.core.network.protocol.AudioCodecType
import com.pttlan.core.network.protocol.AudioEnvelope
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.core.network.protocol.LoginResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assume.assumeTrue
import java.io.File
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Load on a server (31.7): how many people a room holds, above all in host mode on a phone. Not part of the
 * suite: it runs only against the server named by `PTT_LOAD_HOST`, while CPU and memory are read on that host.
 *
 * `PTT_LOAD_HOST=192.168.1.230 PTT_LOAD_CLIENTS=20 ./gradlew :server-core:test --tests '*HostLoadTest*' --rerun`
 *
 * Every client joins the default channel; the first takes the floor and sends one Opus frame every 20 ms, as the
 * app does, and the others count what reaches them. The report goes to `server-core/build/load-report.txt`, with the
 * wall-clock window of the stream, so CPU sampled on the host can be split from the logins before it.
 *
 * The server allows a few logins a minute per address (a real room comes from one address per person), and every
 * client here comes from this machine: a refused login waits and retries, so a big N takes minutes to join.
 */
class HostLoadTest {
    private val host = System.getenv("PTT_LOAD_HOST")
    private val clients = System.getenv("PTT_LOAD_CLIENTS")?.toInt() ?: 10
    private val seconds = System.getenv("PTT_LOAD_SECONDS")?.toInt() ?: 30

    // A real 20 ms Opus frame of a tone, so listeners decode, play and record it as they would speech
    private val chunk = OpusAudioCodec().encode(toneFrame())
    private val pin = System.getenv("PTT_LOAD_PIN")
    private val frame = 20.milliseconds
    private val loginRetry = 13.seconds

    // Well under the server's one-minute ceiling for a turn, with the pause of someone letting go of the button
    private val turn = 30.seconds
    private val turnGap = 1.seconds

    @Test
    fun `a room with many listeners and one speaker`() =
        runBlocking {
            assumeTrue("Set PTT_LOAD_HOST to run the load test", host != null)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val connected = (1..clients).map { index -> connect(scope, index) }
            val (speaker, speakerId) = connected.first()
            val received = connected.drop(1).map { (client, _) -> countAudio(scope, client) }

            // In turns, as people talk: the server takes the floor back after a minute of one turn
            val streamStart = LocalTime.now()
            val start = TimeSource.Monotonic.markNow()
            var sent = 0
            while (start.elapsedNow() < seconds.seconds) {
                takeFloor(scope, speaker, speakerId)
                sent += stream(speaker, speakerId, minOf(turn, seconds.seconds - start.elapsedNow()))
                speaker.sendControlMessage(ControlMessage.StopSpeaking(DEFAULT_CHANNEL_ID, speakerId))
                delay(turnGap)
            }
            val streamEnd = LocalTime.now()
            delay(1.seconds) // the last chunks still on their way

            report(sent, received.map { it.get() }, "$streamStart-$streamEnd")
            connected.forEach { (client, _) -> client.disconnect() }
            scope.cancel()
        }

    private suspend fun takeFloor(
        scope: CoroutineScope,
        speaker: PttWebSocketClient,
        speakerId: String,
    ) {
        // Listening before asking: the flow keeps no history, and a fast server answers before a late collector
        val granted =
            scope.async(start = CoroutineStart.UNDISPATCHED) {
                speaker.controlMessages
                    .filterIsInstance<ControlMessage.SpeakerChanged>()
                    .first { it.userId == speakerId && it.isSpeaking }
            }
        speaker.sendControlMessage(ControlMessage.StartSpeaking(DEFAULT_CHANNEL_ID, speakerId))
        withTimeout(10.seconds) { granted.await() }
    }

    private suspend fun connect(
        scope: CoroutineScope,
        index: Int,
    ): Pair<PttWebSocketClient, String> {
        val client = PttWebSocketClient(createHttpClient())
        val login = login(client, index)
        scope.launch { client.connect(host, DEFAULT_SERVER_PORT, isLocal = true, token = login.token) }
        withTimeout(10.seconds) { client.isConnected.first { it } }
        client.sendControlMessage(ControlMessage.JoinChannel(DEFAULT_CHANNEL_ID, "load$index", login.userId))
        return client to login.userId
    }

    private suspend fun login(
        client: PttWebSocketClient,
        index: Int,
    ): LoginResponse {
        while (true) {
            try {
                return client.login(host!!, DEFAULT_SERVER_PORT, isLocal = true, "load$index", "load-device-$index", pin)
            } catch (_: TooManyAttemptsException) {
                delay(loginRetry)
            }
        }
    }

    private fun countAudio(
        scope: CoroutineScope,
        client: PttWebSocketClient,
    ): AtomicInteger {
        val count = AtomicInteger()
        scope.launch { client.audioChunks.collect { count.incrementAndGet() } }
        return count
    }

    /** Sends a chunk every [frame] for [length], keeping the pace against the clock, and returns how many went out. */
    private suspend fun stream(
        speaker: PttWebSocketClient,
        speakerId: String,
        length: Duration,
    ): Int {
        val start = TimeSource.Monotonic.markNow()
        var sequence = 0
        while (start.elapsedNow() < length) {
            val envelope =
                AudioEnvelope(DEFAULT_CHANNEL_ID, speakerId, sequence, AudioCodecType.OPUS, System.currentTimeMillis())
            speaker.sendAudioChunk(envelope, chunk)
            sequence++
            val due = frame * sequence
            val wait = due - start.elapsedNow()
            if (wait.isPositive()) delay(wait)
        }
        return sequence
    }

    private fun report(
        sent: Int,
        received: List<Int>,
        window: String,
    ) {
        val lossPercent = received.map { 100.0 * (sent - it).coerceAtLeast(0) / sent }
        val text =
            buildString {
                appendLine("host=$host clients=$clients seconds=$seconds chunkBytes=${chunk.size} stream=$window")
                appendLine("sent=$sent chunks by 1 speaker to ${received.size} listeners")
                appendLine("received min=${received.min()} max=${received.max()} avg=${"%.1f".format(Locale.ROOT, received.average())}")
                append("loss worst=${"%.2f".format(Locale.ROOT, lossPercent.max())}%")
                appendLine(" avg=${"%.2f".format(Locale.ROOT, lossPercent.average())}%")
            }
        println(text)
        File("build/load-report.txt").appendText(text + "\n")
    }
}

private const val SAMPLE_RATE = 48_000
private const val FRAME_SAMPLES = 960
private const val TONE_HZ = 440.0
private const val TONE_AMPLITUDE = 8_000.0
private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8

/** 20 ms of a 440 Hz tone as 48 kHz mono 16 bit little-endian PCM, the frame the app captures. */
private fun toneFrame(): ByteArray =
    ByteArray(FRAME_SAMPLES * 2).also { pcm ->
        for (i in 0 until FRAME_SAMPLES) {
            val sample = (TONE_AMPLITUDE * sin(2 * PI * TONE_HZ * i / SAMPLE_RATE)).toInt()
            pcm[i * 2] = (sample and BYTE_MASK).toByte()
            pcm[i * 2 + 1] = ((sample shr BITS_PER_BYTE) and BYTE_MASK).toByte()
        }
    }
