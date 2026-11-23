package com.pttlan.core.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcmFramerTest {
    private val frameBytes = 4

    @Test
    fun lessThanAFrameWaitsForTheNextBlock() {
        val framer = PcmFramer(frameBytes)
        assertTrue(framer.push(byteArrayOf(1, 2, 3)).isEmpty())
        assertEquals(listOf(listOf<Byte>(1, 2, 3, 4)), framer.push(byteArrayOf(4, 5)).map { it.toList() })
    }

    @Test
    fun aLargeBlockBecomesSeveralFramesInOrderAndKeepsTheRest() {
        val framer = PcmFramer(frameBytes)
        val frames = framer.push(ByteArray(10) { it.toByte() })
        assertEquals(2, frames.size)
        assertContentEquals(byteArrayOf(4, 5, 6, 7), frames[1])
        assertContentEquals(byteArrayOf(8, 9, 10, 11), framer.push(byteArrayOf(10, 11)).single())
    }
}
