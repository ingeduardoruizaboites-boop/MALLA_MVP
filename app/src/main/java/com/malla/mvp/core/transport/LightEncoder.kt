package com.malla.mvp.core.transport

object LightEncoder {
    private val PREAMBLE = listOf(1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0)
    private val FRAME_START = listOf(1,1,1,1,0,0,0,0)

    fun encode(text: String): List<Int> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val length = bytes.size.coerceAtMost(255)
        val data = bytes.take(length)
        var checksum = 0
        checksum = checksum xor length
        data.forEach { checksum = checksum xor it.toInt() }
        val bits = mutableListOf<Int>()
        bits.addAll(PREAMBLE)
        bits.addAll(FRAME_START)
        bits.addAll(byteToBits(length.toByte()))
        data.forEach { b -> bits.addAll(byteToBits(b)) }
        bits.addAll(byteToBits(checksum.toByte()))
        return bits
    }

    fun decode(bits: List<Int>): String? {
        if (bits.size < PREAMBLE.size + FRAME_START.size + 8 + 8) return null
        var idx = 0
        val startIdx = bits.windowed(PREAMBLE.size).indexOfFirst { it == PREAMBLE }
        if (startIdx == -1) return null
        idx = startIdx + PREAMBLE.size
        if (bits.drop(idx).take(FRAME_START.size) != FRAME_START) return null
        idx += FRAME_START.size
        val length = bitsToByte(bits.drop(idx).take(8))?.toInt() ?: return null
        if (length < 0 || length > 255) return null
        idx += 8
        val dataBytes = mutableListOf<Byte>()
        for (i in 0 until length) {
            val b = bitsToByte(bits.drop(idx).take(8)) ?: return null
            dataBytes.add(b)
            idx += 8
        }
        val expectedChecksum = bitsToByte(bits.drop(idx).take(8))?.toInt() ?: return null
        var actualChecksum = 0
        actualChecksum = actualChecksum xor length
        dataBytes.forEach { actualChecksum = actualChecksum xor it.toInt() }
        if (expectedChecksum != actualChecksum.toByte().toInt()) return null
        return String(dataBytes.toByteArray(), Charsets.UTF_8)
    }

    private fun byteToBits(b: Byte): List<Int> {
        return (0..7).map { (b.toInt() shr (7-it)) and 1 }.toList()
    }

    private fun bitsToByte(bits: List<Int>): Byte? {
        if (bits.size < 8) return null
        var value = 0
        for (i in 0..7) {
            value = (value shl 1) or (bits[i] and 1)
        }
        return value.toByte()
    }
}
