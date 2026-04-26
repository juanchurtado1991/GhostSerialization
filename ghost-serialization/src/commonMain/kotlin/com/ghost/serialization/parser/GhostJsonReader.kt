package com.ghost.serialization.parser

import com.ghost.serialization.InternalGhostApi
import com.ghost.serialization.exception.GhostJsonException
import com.ghost.serialization.parser.GhostJsonConstants.BACKSLASH
import com.ghost.serialization.parser.GhostJsonConstants.QUOTE
import com.ghost.serialization.parser.GhostJsonConstants.UNTERMINATED_STRING_ERROR
import okio.ByteString

/**
 * High-performance, zero-allocation JSON reader.
 * Core class containing state and low-level navigation.
 * Complex operations are provided via extensions in Subsystem and Numbers.
 */
@InternalGhostApi
class GhostJsonReader(
    @PublishedApi internal var source: GhostSource,
    @PublishedApi internal var limit: Int = source.size,
    var maxDepth: Int = 255,
    var strictMode: Boolean = false,
    var coerceStringsToNumbers: Boolean = false,
    var maxCollectionSize: Int = 10_000_000
) {
    /** Convenience constructor for ByteArray — used by KSP-generated serializers and tests. */
    constructor(
        bytes: ByteArray,
        maxDepth: Int = 255,
        strictMode: Boolean = false,
        coerceStringsToNumbers: Boolean = false,
        maxCollectionSize: Int = 10_000_000
    ) : this(
        createByteArraySource(bytes),
        bytes.size,
        maxDepth,
        strictMode,
        coerceStringsToNumbers,
        maxCollectionSize
    )

    @PublishedApi
    internal var position: Int = 0

    @PublishedApi
    internal var nextTokenByte: Int = -1

    /** Current nesting depth (object/array). Incremented on begin*, decremented on end*. */
    var depth: Int = 0

    @PublishedApi
    internal val stringPool = arrayOfNulls<String>(GhostJsonConstants.STR_POOL_SIZE)

    fun throwError(message: String): Nothing {
        var line = 0
        var column = 0
        val endPos = if (position > source.size) source.size else position
        for (i in 0 until endPos) {
            if (source[i] == GhostJsonConstants.NEWLINE) {
                line++
                column = 0
            } else {
                column++
            }
        }
        throw GhostJsonException("$message at position $position [line $line, col $column]", line, column)
    }

    fun expectByte(expected: Byte) {
        if (peekNextToken() != expected.toInt()) {
            throwError(
                "Expected '${
                    expected.toInt().toChar()
                }' but found ${nextTokenByte.toChar()}"
            )
        }
        internalSkip(1)
    }

    fun internalSkip(n: Int) {
        position += n
        nextTokenByte = -1
    }

    fun skipWhitespace() {
        val nextPos = source.findNextNonWhitespace(position, limit)
        if (nextPos != -1) {
            position = nextPos
            nextTokenByte = source[position].toInt()
        } else {
            position = limit
            nextTokenByte = -1
        }
    }

    fun peekNextToken(): Int {
        if (nextTokenByte != -1) return nextTokenByte
        skipWhitespace()
        return nextTokenByte
    }

    fun peekByte(): Byte = peekNextToken().toByte()

    fun nextNonWhitespace(): Byte {
        val b = peekNextToken()
        if (b == -1) throwError(GhostJsonConstants.ERR_UNEXPECTED_EOF)
        internalSkip(1)
        return b.toByte()
    }

    fun skipAndValidateLiteral(expected: ByteString) {
        val len = expected.size
        // First byte is already peeked/validated by caller usually, but let's be safe
        for (i in 0 until len) {
            if (position >= limit || source[position] != expected[i]) {
                throwError("Expected literal $expected")
            }
            position++
        }
        nextTokenByte = -1
    }

    /**
     * Reads a quoted string into a Kotlin String.
     * Uses optimized paths for ByteArraySource/WasmJsSource.
     */
    fun readQuotedString(): String {
        if (peekNextToken() != QUOTE.toInt()) throwError(GhostJsonConstants.ERR_EXPECTED_QUOTE)
        position++
        nextTokenByte = -1

        val start = position
        val end = source.findClosingQuote(start, limit)

        if (end != -1) {
            val length = end - start
            if (length <= 0) {
                position = end + 1
                return ""
            }
            if (length > GhostJsonConstants.MAX_POOL_STRING_LENGTH) {
                val result = source.decodeToString(start, end)
                position = end + 1
                return result
            }

            // Optimized Rolling Hash with loop unrolling
            var rollingHash = 0
            var i = start

            while (i + 3 < end) {
                rollingHash = (rollingHash shl 5) - rollingHash + (source[i].toInt() and GhostJsonConstants.BYTE_MASK)
                rollingHash = (rollingHash shl 5) - rollingHash + (source[i + 1].toInt() and GhostJsonConstants.BYTE_MASK)
                rollingHash = (rollingHash shl 5) - rollingHash + (source[i + 2].toInt() and GhostJsonConstants.BYTE_MASK)
                rollingHash = (rollingHash shl 5) - rollingHash + (source[i + 3].toInt() and GhostJsonConstants.BYTE_MASK)
                i += 4
            }
            while (i < end) {
                rollingHash = (rollingHash shl 5) - rollingHash + (source[i].toInt() and GhostJsonConstants.BYTE_MASK)
                i++
            }

            val poolIndex = rollingHash and (GhostJsonConstants.STR_POOL_SIZE - 1)
            val cached = stringPool[poolIndex]

            if (cached != null && cached.length == length) {
                // Hot Path: Candidate found, verify bytes with unrolling
                var match = true
                if (length >= 4) {
                    if (cached[0].code != (source[start].toInt() and GhostJsonConstants.BYTE_MASK) ||
                        cached[1].code != (source[start + 1].toInt() and GhostJsonConstants.BYTE_MASK) ||
                        cached[2].code != (source[start + 2].toInt() and GhostJsonConstants.BYTE_MASK) ||
                        cached[3].code != (source[start + 3].toInt() and GhostJsonConstants.BYTE_MASK)
                    ) {
                        match = false
                    }
                }
                if (match) {
                    var j = if (length >= 4) 4 else 0
                    while (j < length) {
                        if (cached[j].code != (source[start + j].toInt() and GhostJsonConstants.BYTE_MASK)) {
                            match = false
                            break
                        }
                        j++
                    }
                    if (match) {
                        position = end + 1
                        return cached
                    }
                }
            }

            // Cold Path: Decode and potentially update pool
            val result = source.decodeToString(start, end)
            stringPool[poolIndex] = result
            position = end + 1
            return result
        }

        // Slow path: manual string building for escapes
        val sb = StringBuilder()
        while (position < limit) {
            val b = source[position++].toInt().toChar()
            if (b == '"') return sb.toString()
            if (b.code in 0..31) throwError(GhostJsonConstants.UNESCAPED_CONTROL_CHAR_ERROR)
            if (b == '\\') {
                if (position >= limit) throwError(GhostJsonConstants.UNTERMINATED_ESCAPE_ERROR)
                val escaped = source[position++].toInt().toChar()
                when (escaped) {
                    'u' -> {
                        if (position + 4 > limit) throwError(GhostJsonConstants.UNTERMINATED_UNICODE_ERROR)
                        val hex = source.decodeToString(position, position + 4)
                        val code = try { hex.toInt(16) } catch (e: Exception) { throwError("Invalid unicode escape: \\u$hex") }
                        position += 4
                        if (code in 0xD800..0xDBFF) {
                            if (position + 6 > limit || source[position].toInt().toChar() != '\\' || source[position + 1].toInt().toChar() != 'u') {
                                throwError("Lone high surrogate: \\u$hex")
                            }
                            position += 2
                            val lowHex = source.decodeToString(position, position + 4)
                            val lowCode = try { lowHex.toInt(16) } catch (e: Exception) { throwError("Invalid low surrogate: \\u$lowHex") }
                            if (lowCode !in 0xDC00..0xDFFF) throwError("Lone high surrogate: \\u$hex")
                            position += 4
                            sb.append(code.toChar())
                            sb.append(lowCode.toChar())
                        } else {
                            sb.append(code.toChar())
                        }
                    }

                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000c')
                    else -> sb.append(escaped)
                }
            } else {
                sb.append(b)
            }
        }
        throwError(UNTERMINATED_STRING_ERROR)
    }

    fun skipQuotedString() {
        if (peekNextToken() != QUOTE.toInt()) throwError(GhostJsonConstants.ERR_EXPECTED_QUOTE)
        position++
        nextTokenByte = -1

        val start = position
        val end = source.findClosingQuote(start, limit)
        if (end != -1) {
            position = end + 1
            return
        }

        // Slow path: string has escapes or control chars. We need full validation.
        position = start - 1 // Reset position to before the quote
        nextTokenByte = QUOTE.toInt()
        readQuotedString().ignore() // Let the actual reader do the heavy validation
    }

    fun reset(newData: ByteArray, newLimit: Int = newData.size) {
        this.source = createByteArraySource(newData)
        this.position = 0
        this.limit = newLimit
        this.nextTokenByte = -1
    }

    fun reset(newSource: GhostSource, newLimit: Int = newSource.size) {
        this.source = newSource
        this.position = 0
        this.limit = newLimit
        this.nextTokenByte = -1
    }
}