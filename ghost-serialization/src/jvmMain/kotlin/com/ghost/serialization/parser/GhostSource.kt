package com.ghost.serialization.parser

import com.ghost.serialization.InternalGhostApi

@InternalGhostApi
class JvmByteArraySource(val data: ByteArray) : GhostSource {
    override val size: Int get() = data.size
    override fun get(index: Int): Byte = data[index]
    override fun decodeToString(start: Int, end: Int): String = data.decodeToString(start, end)
    override fun contentEquals(start: Int, expected: ByteArray): Boolean {
        if (start + expected.size > size) return false
        return java.util.Arrays.equals(data, start, start + expected.size, expected, 0, expected.size)
    }

    override fun findNextNonWhitespace(position: Int, limit: Int): Int {
        val d = data
        var pos = position
        while (pos < limit) {
            if (d[pos] > 32) return pos
            pos++
        }
        return -1
    }

    override fun findClosingQuote(position: Int, limit: Int): Int {
        val d = data
        var pos = position
        while (pos < limit) {
            val b = d[pos]
            if (b == 34.toByte()) return pos
            if (b == 92.toByte()) return -1
            if (b >= 0 && b <= 31) return -1
            pos++
        }
        return -1
    }
}

@InternalGhostApi
actual fun createByteArraySource(data: ByteArray): GhostSource = JvmByteArraySource(data)
