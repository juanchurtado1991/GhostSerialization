package com.ghost.serialization.parser

import com.ghost.serialization.InternalGhostApi
import okio.ByteString.Companion.encodeUtf8
import kotlin.math.pow

@PublishedApi
internal object GhostJsonConstants {
    const val STRING_BUILDER_CAPACITY = 128
    const val UNTERMINATED_STRING_ERROR = "Unterminated string"
    const val TRUNCATED_LITERAL_ERROR = "Truncated literal at end of source"
    const val UNEXPECTED_EOF_ERROR = "Unexpected EOF"
    const val UNTERMINATED_ESCAPE_ERROR = "Unterminated escape sequence"
    const val UNTERMINATED_UNICODE_ERROR = "Unterminated unicode escape"
    const val UNESCAPED_CONTROL_CHAR_ERROR = "Unescaped control character in string"
    const val STRICT_MODE_UNKNOWN_FIELD = "Unknown field in strict mode: "
    const val ERR_EXPECTED_BEGIN_OBJ = "Expected '{'"
    const val ERR_EXPECTED_END_OBJ = "Expected '}'"
    const val ERR_EXPECTED_BEGIN_ARR = "Expected '['"
    const val ERR_EXPECTED_END_ARR = "Expected ']'"
    const val ERR_TRAILING_COMMA = "Trailing comma"
    const val ERR_EXPECTED_COLON = "Expected ':'"
    const val ERR_EXPECTED_BOOLEAN = "Expected boolean but found "
    const val ERR_EXPECTED_KEY = "Expected key but found "
    const val ERR_UNTERMINATED_KEY = "Unterminated key"
    const val ERR_EXPECTED_STRING = "Expected string"
    const val ERR_MAX_COLLECTION_SIZE = "Collection size exceeds maximum allowed"
    const val ERR_EXPECTED_COMMA_OR_CLOSE_ARR = "Expected ',' or ']'"
    const val ERR_EXPECTED_COMMA_OR_CLOSE_OBJ = "Expected ',' or '}'"
    const val ERR_UNEXPECTED_EOF = "Unexpected end of input"
    const val ERR_EXPECTED_QUOTE = "Expected '\"'"
    const val PATH_ROOT = "$"
    const val COLON_QUOTE = "\":"
    const val UNICODE_PREFIX = "\\u"
    const val ZERO_CHAR = "0"
    const val ERR_NON_FINITE = "JSON does not support non-finite numbers like NaN or Infinity"
    const val ERR_DEPTH_EXCEEDED = "Reached maximum recursion depth"


    @PublishedApi
    internal val TRUE_BYTES = "true".encodeUtf8()
    @PublishedApi
    internal val FALSE_BYTES = "false".encodeUtf8()
    @PublishedApi
    internal val NULL_BYTES = "null".encodeUtf8()

    const val NEWLINE = '\n'.code.toByte()

    const val COMMA = ','.code.toByte()
    const val COLON = ':'.code.toByte()
    const val QUOTE = '"'.code.toByte()
    const val BACKSLASH = '\\'.code.toByte()
    const val OPEN_OBJ = '{'.code.toByte()
    const val CLOSE_OBJ = '}'.code.toByte()
    const val OPEN_ARR = '['.code.toByte()
    const val CLOSE_ARR = ']'.code.toByte()
    const val NULL_CHAR = 'n'.code.toByte()
    const val TRUE_CHAR = 't'.code.toByte()
    const val FALSE_CHAR = 'f'.code.toByte()
    const val MINUS = '-'.code.toByte()
    const val PLUS = '+'.code.toByte()
    const val DOT = '.'.code.toByte()
    const val ZERO = '0'.code.toByte()
    const val NINE = '9'.code.toByte()
    const val EXP_LOWER = 'e'.code.toByte()
    const val EXP_UPPER = 'E'.code.toByte()
    const val BYTE_MASK = 0xFF

    // --- STR POOL METRICS ---
    const val STR_POOL_SIZE = 2048
    val MAX_POOL_STRING_LENGTH get() = GhostHeuristics.maxStringPoolLength

    val POWERS_OF_TEN = DoubleArray(309).apply {
        for (i in indices) this[i] = 10.0.pow(i.toDouble())
    }

    val INVERSE_POWERS_OF_TEN = DoubleArray(309).apply {
        for (i in indices) this[i] = 1.0 / 10.0.pow(i.toDouble())
    }

    val BLOCK_ESCAPE = ByteArray(128).apply {
        for (i in 0..31) this[i] = 1 // Control characters
        this['"'.code] = 1
        this['\\'.code] = 1
    }

    val IS_TERMINATOR = BooleanArray(128).apply {
        ",}] \n\r\t:".forEach { this[it.code] = true }
    }



    internal object FormatUtils {
        val DIGIT_TENS = ByteArray(100)
        val DIGIT_ONES = ByteArray(100)

        init {
            for (i in 0 until 100) {
                DIGIT_TENS[i] = ((i / 10) + 48).toByte()
                DIGIT_ONES[i] = ((i % 10) + 48).toByte()
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
@InternalGhostApi
inline fun Any?.ignore() {}
