package com.pttlan.core.audio

import co.touchlab.kermit.Logger
import eu.buney.kopus.OpusApplication
import eu.buney.kopus.OpusDecoder
import eu.buney.kopus.OpusEncoder

class OpusAudioCodec(
    sampleRate: Int = 48000,
    private val channels: Int = 1,
) : AudioCodec {
    private val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Voip)
    private val decoder = OpusDecoder(sampleRate, channels)
    private val logger = Logger.withTag("audio")

    override fun encode(pcm: ByteArray): ByteArray {
        val shortArray = pcm.toShortArray()
        val frameSize = shortArray.size / channels
        val outData = ByteArray(4000)
        val encodedBytes =
            try {
                encoder.encode(
                    inPcm = shortArray,
                    inPcmOffset = 0,
                    frameSize = frameSize,
                    outData = outData,
                    outDataOffset = 0,
                    maxDataBytes = outData.size,
                )
            } catch (e: Exception) {
                // Opus only accepts 120/240/480/960/1920/2880 samples per frame; silence here used to look
                // exactly like a working encoder sending nothing.
                logger.e(e) { "Falha ao codificar frame de ${shortArray.size} amostras em Opus" }
                0
            }
        return if (encodedBytes > 0) outData.copyOfRange(0, encodedBytes) else ByteArray(0)
    }

    override fun decode(encoded: ByteArray): ByteArray {
        val frameSize = 1920
        val outPcm = ShortArray(frameSize * channels)
        val decodedSamples =
            try {
                decoder.decode(
                    inData = encoded,
                    inDataOffset = 0,
                    len = encoded.size,
                    outPcm = outPcm,
                    outPcmOffset = 0,
                    frameSize = frameSize,
                    decodeFec = false,
                )
            } catch (e: Exception) {
                logger.e(e) { "Falha ao decodificar ${encoded.size} bytes de Opus" }
                0
            }
        return if (decodedSamples > 0) {
            outPcm.copyOfRange(0, decodedSamples * channels).toByteArray()
        } else {
            ByteArray(0)
        }
    }

    private fun ByteArray.toShortArray(): ShortArray {
        val result = ShortArray(this.size / 2)
        for (i in result.indices) {
            val low = this[i * 2].toInt() and 0xFF
            val high = this[i * 2 + 1].toInt() and 0xFF
            result[i] = ((high shl 8) or low).toShort()
        }
        return result
    }

    private fun ShortArray.toByteArray(): ByteArray {
        val result = ByteArray(this.size * 2)
        for (i in this.indices) {
            val value = this[i].toInt()
            result[i * 2] = (value and 0xFF).toByte()
            result[i * 2 + 1] = ((value ushr 8) and 0xFF).toByte()
        }
        return result
    }
}
