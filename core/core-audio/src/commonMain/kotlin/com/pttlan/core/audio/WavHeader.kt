package com.pttlan.core.audio

private const val HEADER_BYTES = 44
private const val RIFF_SIZE_EXCLUDED = 8
private const val FMT_CHUNK_BYTES = 16
private const val FORMAT_PCM: Short = 1
private const val BITS_PER_SAMPLE = 16
private const val BITS_PER_BYTE = 8
private const val BYTE_MASK = 0xFF

/**
 * The 44 byte header that turns raw 16 bit little-endian PCM, as the history stores it, into a `.wav` any
 * player opens: write it, then the samples.
 */
fun wavHeader(
    dataBytes: Int,
    sampleRate: Int = 48_000,
    channels: Int = 1,
): ByteArray {
    val blockAlign = channels * BITS_PER_SAMPLE / BITS_PER_BYTE
    val header = ArrayList<Byte>(HEADER_BYTES)

    fun text(value: String) = value.encodeToByteArray().forEach { header += it }

    fun littleEndian(
        value: Int,
        bytes: Int,
    ) = repeat(bytes) { header += ((value shr (BITS_PER_BYTE * it)) and BYTE_MASK).toByte() }

    fun int(value: Int) = littleEndian(value, Int.SIZE_BYTES)

    fun short(value: Int) = littleEndian(value, Short.SIZE_BYTES)

    text("RIFF")
    int(HEADER_BYTES - RIFF_SIZE_EXCLUDED + dataBytes)
    text("WAVE")
    text("fmt ")
    int(FMT_CHUNK_BYTES)
    short(FORMAT_PCM.toInt())
    short(channels)
    int(sampleRate)
    int(sampleRate * blockAlign)
    short(blockAlign)
    short(BITS_PER_SAMPLE)
    text("data")
    int(dataBytes)
    return header.toByteArray()
}
