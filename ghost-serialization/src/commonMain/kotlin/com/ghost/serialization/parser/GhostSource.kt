package com.ghost.serialization.parser

import com.ghost.serialization.InternalGhostApi

@InternalGhostApi
interface GhostSource {
    val size: Int
    operator fun get(index: Int): Byte
    fun decodeToString(start: Int, end: Int): String
    fun contentEquals(start: Int, expected: ByteArray): Boolean
    
    /**
     * Finds the next non-whitespace byte (> 32) starting from [position].
     * Returns the position or -1 if not found.
     */
    fun findNextNonWhitespace(position: Int, limit: Int): Int {
        var pos = position
        while (pos < limit) {
            if (get(pos) > 32) return pos
            pos++
        }
        return -1
    }

    /**
     * Finds the closing quote (") starting from [position], stopping at [limit].
     * If a backslash (\) is encountered, it returns -1 to signal the slow path is needed.
     */
    fun findClosingQuote(position: Int, limit: Int): Int {
        var pos = position
        while (pos < limit) {
            val b = get(pos)
            if (b == 34.toByte()) return pos // '"'
            if (b == 92.toByte()) return -1 // '\\'
            if (b >= 0 && b <= 31) return -1 // Control characters
            pos++
        }
        return -1
    }
}

@InternalGhostApi
expect fun createByteArraySource(data: ByteArray): GhostSource
