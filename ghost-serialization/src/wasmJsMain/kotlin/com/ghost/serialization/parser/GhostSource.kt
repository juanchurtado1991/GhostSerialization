@file:OptIn(ExperimentalWasmJsInterop::class)

package com.ghost.serialization.parser

import com.ghost.serialization.InternalGhostApi

@InternalGhostApi
class WasmByteArraySource(val data: ByteArray) : GhostSource {
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
class WasmJsSource(val data: JsAny) : GhostSource {
    override val size: Int get() = getPlatformSize(data)
    override fun get(index: Int): Byte = getPlatformByte(data, index)
    override fun decodeToString(start: Int, end: Int): String =
        decodePlatformString(data, start, end)

    override fun findNextNonWhitespace(position: Int, limit: Int): Int =
        findPlatformNextNonWhitespace(data, position, limit)

    override fun findClosingQuote(position: Int, limit: Int): Int =
        findPlatformClosingQuote(data, position, limit)

    override fun contentEquals(start: Int, expected: ByteArray): Boolean {
        if (start + expected.size > size) return false
        for (i in expected.indices) {
            if (get(start + i) != expected[i]) return false
        }
        return true
    }
}

@JsFun("(a) => a.length")
private external fun getPlatformSize(a: JsAny): Int

@JsFun("(a, i) => a[i]")
private external fun getPlatformByte(a: JsAny, i: Int): Byte

@JsFun("(a, s, e) => new TextDecoder().decode(a.subarray(s, e))")
private external fun decodePlatformString(a: JsAny, s: Int, e: Int): String

@JsFun("(a, p, l) => { for (let i = p; i < l; i++) if (a[i] > 32) return i; return -1; }")
private external fun findPlatformNextNonWhitespace(a: JsAny, p: Int, l: Int): Int

@JsFun("(a, p, l) => { for (let i = p; i < l; i++) { let b = a[i]; if (b === 34) return i; if (b === 92) return -1; if (b >= 0 && b <= 31) return -1; } return -1; }")
private external fun findPlatformClosingQuote(a: JsAny, p: Int, l: Int): Int


@InternalGhostApi
actual fun createByteArraySource(data: ByteArray): GhostSource = WasmByteArraySource(data)

@InternalGhostApi
fun createJsSource(data: JsAny): GhostSource = WasmJsSource(data)
