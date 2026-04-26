@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(InternalGhostApi::class)

package com.ghost.serialization.parser

import com.ghost.serialization.InternalGhostApi
import com.ghost.serialization.parser.GhostJsonConstants.CLOSE_ARR
import com.ghost.serialization.parser.GhostJsonConstants.CLOSE_OBJ
import com.ghost.serialization.parser.GhostJsonConstants.COLON
import com.ghost.serialization.parser.GhostJsonConstants.COMMA
import com.ghost.serialization.parser.GhostJsonConstants.DOT
import com.ghost.serialization.parser.GhostJsonConstants.MINUS
import com.ghost.serialization.parser.GhostJsonConstants.NINE
import com.ghost.serialization.parser.GhostJsonConstants.OPEN_ARR
import com.ghost.serialization.parser.GhostJsonConstants.OPEN_OBJ
import com.ghost.serialization.parser.GhostJsonConstants.PLUS
import com.ghost.serialization.parser.GhostJsonConstants.QUOTE
import com.ghost.serialization.parser.GhostJsonConstants.ZERO


fun GhostJsonReader.beginObject() {
    if (nextNonWhitespace() != OPEN_OBJ) throwError(GhostJsonConstants.ERR_EXPECTED_BEGIN_OBJ)
    depth++
    if (depth > maxDepth) throwError(GhostJsonConstants.ERR_DEPTH_EXCEEDED)
}


fun GhostJsonReader.endObject() {
    if (nextNonWhitespace() != CLOSE_OBJ) throwError(GhostJsonConstants.ERR_EXPECTED_END_OBJ)
    depth--
}


fun GhostJsonReader.beginArray() {
    if (nextNonWhitespace() != OPEN_ARR) throwError(GhostJsonConstants.ERR_EXPECTED_BEGIN_ARR)
    depth++
    if (depth > maxDepth) throwError(GhostJsonConstants.ERR_DEPTH_EXCEEDED)
}


fun GhostJsonReader.endArray() {
    if (nextNonWhitespace() != CLOSE_ARR) throwError(GhostJsonConstants.ERR_EXPECTED_END_ARR)
    depth--
}


fun GhostJsonReader.hasNext(): Boolean {
    val token = peekNextToken()
    if (token == CLOSE_ARR.toInt() || token == CLOSE_OBJ.toInt() || token == -1) return false
    if (token == COMMA.toInt()) {
        internalSkip(1)
        val next = peekNextToken()
        if (next == CLOSE_ARR.toInt() || next == CLOSE_OBJ.toInt()) {
            throwError(GhostJsonConstants.ERR_TRAILING_COMMA)
        }
    }
    return true
}


fun GhostJsonReader.nextKey(): String? {
    val token = peekNextToken()
    if (token == CLOSE_OBJ.toInt()) return null
    if (token == COMMA.toInt()) {
        internalSkip(1)
        if (peekNextToken() == CLOSE_OBJ.toInt()) throwError(GhostJsonConstants.ERR_TRAILING_COMMA)
    }
    return readQuotedString()
}


fun GhostJsonReader.consumeKeySeparator() {
    if (nextNonWhitespace() != COLON) throwError(GhostJsonConstants.ERR_EXPECTED_COLON)
}


fun GhostJsonReader.consumeArraySeparator() {
    if (peekNextToken() == COMMA.toInt()) {
        internalSkip(1)
    }
}


fun GhostJsonReader.nextBoolean(): Boolean {
    val token = peekNextToken()
    return when (token) {
        GhostJsonConstants.TRUE_CHAR.toInt() -> {
            skipAndValidateLiteral(GhostJsonConstants.TRUE_BYTES)
            true
        }

        GhostJsonConstants.FALSE_CHAR.toInt() -> {
            skipAndValidateLiteral(GhostJsonConstants.FALSE_BYTES)
            false
        }

        else -> throwError("${GhostJsonConstants.ERR_EXPECTED_BOOLEAN}${token.toChar()}")
    }
}


fun GhostJsonReader.nextString(): String = readQuotedString()


fun GhostJsonReader.isNextNullValue(): Boolean =
    peekNextToken() == GhostJsonConstants.NULL_CHAR.toInt()


fun GhostJsonReader.consumeNull() {
    skipAndValidateLiteral(GhostJsonConstants.NULL_BYTES)
}


fun GhostJsonReader.selectNameAndConsume(options: JsonReaderOptions): Int {
    if (peekNextToken() == CLOSE_OBJ.toInt()) return -1
    if (peekNextToken() == COMMA.toInt()) {
        internalSkip(1)
        if (peekNextToken() == CLOSE_OBJ.toInt()) throwError(GhostJsonConstants.ERR_TRAILING_COMMA)
    }

    if (peekNextToken() != QUOTE.toInt()) throwError("${GhostJsonConstants.ERR_EXPECTED_KEY}${nextTokenByte.toChar()}")

    val start = position + 1
    val end = source.findClosingQuote(start, limit)
    if (end == -1) throwError(GhostJsonConstants.ERR_UNTERMINATED_KEY)

    val len = end - start

    // Multi-byte hashing implementation
    var key = 0
    if (len >= 1) key = key or (source[start].toInt() and GhostJsonConstants.BYTE_MASK)
    if (len >= 2) key = key or ((source[start + 1].toInt() and GhostJsonConstants.BYTE_MASK) shl 8)
    if (len >= 3) key = key or ((source[start + 2].toInt() and GhostJsonConstants.BYTE_MASK) shl 16)
    if (len >= 4) key = key or ((source[start + 3].toInt() and GhostJsonConstants.BYTE_MASK) shl 24)

    val h = ((key * options.multiplier + len) shr options.shift) and 1023
    val index = options.dispatch[h]

    if (index != -1) {
        val expected = options.rawBytes[index]
        if (expected.size == len) {
            if (source.contentEquals(start, expected)) {
                position = end + 1
                nextTokenByte = -1
                consumeKeySeparator()
                return index
            }
        }
    }

    // No match found: skip the key and consume the separator
    position = end + 1
    nextTokenByte = -1
    consumeKeySeparator()
    return -2
}


fun GhostJsonReader.selectString(options: JsonReaderOptions): Int {
    // Handle end of object
    val tok = peekNextToken()
    if (tok == CLOSE_OBJ.toInt()) return -1
    // Skip leading comma between fields
    if (tok == COMMA.toInt()) {
        internalSkip(1)
        if (peekNextToken() == CLOSE_OBJ.toInt()) throwError(GhostJsonConstants.ERR_TRAILING_COMMA)
    }

    if (peekNextToken() != QUOTE.toInt()) throwError(GhostJsonConstants.ERR_EXPECTED_STRING)

    val start = position + 1
    val end = source.findClosingQuote(start, limit)
    if (end == -1) throwError(GhostJsonConstants.UNTERMINATED_STRING_ERROR)

    val len = end - start

    var key = 0
    if (len >= 1) key = key or (source[start].toInt() and GhostJsonConstants.BYTE_MASK)
    if (len >= 2) key = key or ((source[start + 1].toInt() and GhostJsonConstants.BYTE_MASK) shl 8)
    if (len >= 3) key = key or ((source[start + 2].toInt() and GhostJsonConstants.BYTE_MASK) shl 16)
    if (len >= 4) key = key or ((source[start + 3].toInt() and GhostJsonConstants.BYTE_MASK) shl 24)

    val h = ((key * options.multiplier + len) shr options.shift) and 1023
    val index = options.dispatch[h]

    if (index != -1) {
        val expected = options.rawBytes[index]
        if (expected.size == len) {
            var match = true
            for (i in 0 until len) {
                if (source[start + i] != expected[i]) {
                    match = false
                    break
                }
            }
            if (match) {
                position = end + 1
                nextTokenByte = -1
                return index
            }
        }
    }

    // Unknown field — advance past the closing quote and return -2
    position = end + 1
    nextTokenByte = -1
    if (strictMode) {
        val unknownKey = source.decodeToString(start, end)
        throwError("${GhostJsonConstants.STRICT_MODE_UNKNOWN_FIELD}$unknownKey")
    }
    return -2
}

/**
 * Searches for a specific key in the current object without fully consuming it.
 * Used for sealed class discriminators.
 * Highly optimized to avoid unnecessary allocations.
 */
fun GhostJsonReader.peekStringField(name: String): String? {
    val savedPos = position
    val savedToken = nextTokenByte

    try {
        if (peekNextToken() != OPEN_OBJ.toInt()) return null
        internalSkip(1)

        while (hasNext()) {
            val key = nextKey() ?: break
            consumeKeySeparator()
            if (key == name) {
                return if (peekNextToken() == QUOTE.toInt()) readQuotedString() else null
            }
            skipValue()
        }
    } catch (e: Exception) {
        // Silently fail and restore
    } finally {
        position = savedPos
        nextTokenByte = savedToken
    }
    return null
}

fun GhostJsonReader.skipValue() {
    val token = peekNextToken()
    when (token) {
        OPEN_OBJ.toInt() -> {
            beginObject()
            while (hasNext()) {
                nextKey().ignore()
                consumeKeySeparator()
                skipValue()
            }
            endObject()
        }

        OPEN_ARR.toInt() -> {
            beginArray()
            while (hasNext()) {
                skipValue()
            }
            endArray()
        }

        QUOTE.toInt() -> skipQuotedString()

        GhostJsonConstants.TRUE_CHAR.toInt() ->
            skipAndValidateLiteral(GhostJsonConstants.TRUE_BYTES)
        GhostJsonConstants.FALSE_CHAR.toInt() ->
            skipAndValidateLiteral(GhostJsonConstants.FALSE_BYTES)
        GhostJsonConstants.NULL_CHAR.toInt() ->
            skipAndValidateLiteral(GhostJsonConstants.NULL_BYTES)

        else -> {
            // Strictly validate and consume the number
            nextDouble().ignore()
        }
    }
}

fun GhostJsonReader.checkCollectionSize(size: Int) {
    if (size > maxCollectionSize) {
        throwError("${GhostJsonConstants.ERR_MAX_COLLECTION_SIZE} ($maxCollectionSize)")
    }
}

inline fun <T> GhostJsonReader.readList(itemParser: () -> T): List<T> {
    beginArray()
    if (peekNextToken() == CLOSE_ARR.toInt()) {
        endArray()
        return emptyList()
    }
    val list = mutableListOf<T>()
    while (true) {
        list.add(itemParser())
        val next = peekNextToken()
        if (next == CLOSE_ARR.toInt()) {
            endArray()
            break
        }
        if (next != COMMA.toInt()) {
            throwError("${GhostJsonConstants.ERR_EXPECTED_COMMA_OR_CLOSE_ARR} but found ${next.toChar()}")
        }
        internalSkip(1)
        checkCollectionSize(list.size)
    }
    return list
}

inline fun <K, V> GhostJsonReader.readMap(
    keyParser: () -> K,
    valueParser: () -> V
): Map<K, V> {
    beginObject()
    if (peekNextToken() == CLOSE_OBJ.toInt()) {
        endObject()
        return emptyMap()
    }
    val map = mutableMapOf<K, V>()
    while (true) {
        val key = keyParser()
        consumeKeySeparator()
        val value = valueParser()
        map[key] = value
        val next = peekNextToken()
        if (next == CLOSE_OBJ.toInt()) {
            endObject()
            break
        }
        if (next != COMMA.toInt()) {
            throwError("${GhostJsonConstants.ERR_EXPECTED_COMMA_OR_CLOSE_OBJ} but found ${next.toChar()}")
        }
        internalSkip(1)
        checkCollectionSize(map.size)
    }
    return map
}
