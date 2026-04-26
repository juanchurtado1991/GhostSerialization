package com.ghost.serialization.parser

import com.ghost.serialization.InternalGhostApi

@InternalGhostApi
class AndroidByteArraySource(val data: ByteArray) : GhostSource {
    override val size: Int get() = data.size
    override fun get(index: Int): Byte = data[index]
    override fun decodeToString(start: Int, end: Int): String = data.decodeToString(start, end)
    override fun contentEquals(start: Int, expected: ByteArray): Boolean {
        if (start + expected.size > size) return false
        for (i in expected.indices) {
            if (data[start + i] != expected[i]) return false
        }
        return true
    }
}

@InternalGhostApi
actual fun createByteArraySource(data: ByteArray): GhostSource = AndroidByteArraySource(data)
