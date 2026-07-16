package com.pttlan.core.common.crypto

class AudioCrypto(
    private val key: ByteArray = defaultKey,
) {
    private var i = 0
    private var j = 0
    private val s = IntArray(256)

    init {
        for (k in 0..255) {
            s[k] = k
        }
        var k = 0
        for (idx in 0..255) {
            k = (k + s[idx] + key[idx % key.size].toInt().and(0xFF)) and 0xFF
            val temp = s[idx]
            s[idx] = s[k]
            s[k] = temp
        }
    }

    fun process(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (idx in data.indices) {
            i = (i + 1) and 0xFF
            j = (j + s[i]) and 0xFF
            val temp = s[i]
            s[i] = s[j]
            s[j] = temp
            val k = s[(s[i] + s[j]) and 0xFF]
            result[idx] = (data[idx].toInt() xor k).toByte()
        }
        return result
    }

    companion object {
        private val defaultKey =
            byteArrayOf(
                0x2b,
                0x7e,
                0x15,
                0x16,
                0x28,
                0xae.toByte(),
                0xd2.toByte(),
                0xa6.toByte(),
                0xab.toByte(),
                0xf7.toByte(),
                0x15,
                0x88.toByte(),
                0x09,
                0xcf.toByte(),
                0x4f,
                0x3c,
            )
    }
}
