package com.pttlan.core.audio

import eu.buney.kopus.OpusApplication
import eu.buney.kopus.OpusDecoder
import eu.buney.kopus.OpusEncoder

class OpusAudioCodec(
    sampleRate: Int = 16000,
    private val channels: Int = 1,
) : AudioCodec {
    private val encoder = OpusEncoder(sampleRate, channels, OpusApplication.Voip)
    private val decoder = OpusDecoder(sampleRate, channels)

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
            } catch (_: Exception) {
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
            } catch (_: Exception) {
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
